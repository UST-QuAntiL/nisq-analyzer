/*******************************************************************************
 * Copyright (c) 2021 University of Stuttgart
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.planqk.nisq.analyzer.core.prioritization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.CompilationJob;
import org.planqk.nisq.analyzer.core.model.CompilationResult;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.CompilationJobRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionJobRepository;
import org.planqk.nisq.analyzer.core.repository.xmcda.XmcdaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xmcda.v2.Alternative;
import org.xmcda.v2.AlternativeOnCriteriaPerformances;
import org.xmcda.v2.Alternatives;
import org.xmcda.v2.Criteria;
import org.xmcda.v2.CriteriaValues;
import org.xmcda.v2.Criterion;
import org.xmcda.v2.ObjectFactory;
import org.xmcda.v2.PerformanceTable;
import org.xmcda.v2.Value;
import org.xmcda.v2.XMCDA;

import lombok.RequiredArgsConstructor;

/**
 * Utility to retrieve data of a NISQ Analyzer job, i.e., Analysis Job, Compilation Job, or QPU Selection Job
 *
 * FIXME: Unify different jobs/results to remove code duplicates
 */
@Service
@RequiredArgsConstructor
public class JobDataExtractor {

    private final static Logger LOG = LoggerFactory.getLogger(JobDataExtractor.class);

    private final QpuSelectionJobRepository qpuSelectionJobRepository;

    private final AnalysisJobRepository analysisJobRepository;

    private final CompilationJobRepository compilationJobRepository;

    private final XmcdaRepository xmcdaRepository;

    /**
     * Get the required information to run MCDA methods from different kinds of NISQ Analyzer jobs
     *
     * @param jobId      the ID of the job to retrieve the information from
     * @param mcdaMethod the MCDA method to retrieve the data for
     * @return the retrieved job information
     */
    public McdaInformation getJobInformationFromUuid(UUID jobId, String mcdaMethod) {
        LOG.debug("Retrieving job information about job with ID: {}", jobId);

        Optional<QpuSelectionJob> qpuSelectionJobOptional = qpuSelectionJobRepository.findById(jobId);
        if (qpuSelectionJobOptional.isPresent()) {
            LOG.debug("Retrieving information from QPU selection job!");
            return getFromQpuSelection(qpuSelectionJobOptional.get(), mcdaMethod);
        }

        Optional<AnalysisJob> analysisJobOptional = analysisJobRepository.findById(jobId);
        if (analysisJobOptional.isPresent()) {
            LOG.debug("Retrieving information from analysis job!");
            return getFromAnalysis(analysisJobOptional.get(), mcdaMethod);
        }

        Optional<CompilationJob> compilationJobOptional = compilationJobRepository.findById(jobId);
        if (compilationJobOptional.isPresent()) {
            LOG.debug("Retrieving information from compilation job!");
            return getFromCompilation(compilationJobOptional.get(), mcdaMethod);
        }

        LOG.error("Unable to find QPU selection, analysis, or compilation job for ID: {}", jobId);
        return null;
    }

    private McdaInformation getFromQpuSelection(QpuSelectionJob qpuSelectionJob, String mcdaMethod) {
        if (!qpuSelectionJob.isReady()) {
            LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
            return null;
        }

        // retrieve required information for the alternatives and performances
        Alternatives alternatives = new Alternatives();
        PerformanceTable performances = new PerformanceTable();
        LOG.debug("QPU selection job contains {} results for the ranking!", qpuSelectionJob.getJobResults().size());
        for (QpuSelectionResult result : qpuSelectionJob.getJobResults()) {

            // add alternative representing the QPU selection result
            String name = result.getQpu() + "-" + result.getUsedCompiler() + "-" + result.getCircuitName();
            alternatives.getDescriptionOrAlternative().add(createAlternative(result.getId(), name));

            // add performances related to the QPU selection result
            AlternativeOnCriteriaPerformances alternativePerformances = new AlternativeOnCriteriaPerformances();
            alternativePerformances.setAlternativeID(result.getId().toString());
            List<AlternativeOnCriteriaPerformances.Performance> performanceList = alternativePerformances.getPerformance();
            for (Criterion criterion : xmcdaRepository.findAll()) {

                if (CriteriaConstants.QPU_CRITERION.contains(criterion.getName())) {
                    LOG.debug("Retrieving performance data for criterion {} from QPU!", criterion.getName());
                    performanceList.add(createPerformanceForCircuitCriterion(result, criterion));
                } else if (CriteriaConstants.CIRCUIT_CRITERION.contains(criterion.getName())) {
                    LOG.debug("Retrieving performance data for criterion {} from circuit analysis!", criterion.getName());
                    performanceList.add(createPerformanceForQpuCriterion(criterion));
                } else {
                    LOG.error("Criterion with name {} defined in criteria.xml but retrieval of corresponding data is currently not supported!",
                            criterion.getName());
                }
            }
            performances.getAlternativePerformances().add(alternativePerformances);
        }
        LOG.debug("Retrieved job information contains {} alternatives and {} performances!", alternatives.getDescriptionOrAlternative().size(),
                performances.getAlternativePerformances().size());

        return wrapMcdaInformation(alternatives, performances, mcdaMethod);
    }

    private McdaInformation getFromAnalysis(AnalysisJob analysisJob, String mcdaMethod) {
        if (!analysisJob.isReady()) {
            LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
            return null;
        }

        // retrieve required information for the alternatives and performances
        Alternatives alternatives = new Alternatives();
        PerformanceTable performances = new PerformanceTable();
        LOG.debug("Analysis job contains {} results for the ranking!", analysisJob.getJobResults().size());
        for (AnalysisResult result : analysisJob.getJobResults()) {

            // add alternative representing the analysis result
            String name = result.getQpu() + "-" + result.getCompiler() + "-" + result.getImplementation().getName();
            alternatives.getDescriptionOrAlternative().add(createAlternative(result.getId(), name));

            // add performances related to the analysis result
            AlternativeOnCriteriaPerformances alternativePerformances = new AlternativeOnCriteriaPerformances();
            alternativePerformances.setAlternativeID(result.getId().toString());
            List<AlternativeOnCriteriaPerformances.Performance> performanceList = alternativePerformances.getPerformance();
            for (Criterion criterion : xmcdaRepository.findAll()) {

                if (CriteriaConstants.QPU_CRITERION.contains(criterion.getName())) {
                    LOG.debug("Retrieving performance data for criterion {} from QPU!", criterion.getName());
                    performanceList.add(createPerformanceForCircuitCriterion(result, criterion));
                } else if (CriteriaConstants.CIRCUIT_CRITERION.contains(criterion.getName())) {
                    LOG.debug("Retrieving performance data for criterion {} from circuit analysis!", criterion.getName());
                    performanceList.add(createPerformanceForQpuCriterion(criterion));
                } else {
                    LOG.error("Criterion with name {} defined in criteria.xml but retrieval of corresponding data is currently not supported!",
                            criterion.getName());
                }
            }
            performances.getAlternativePerformances().add(alternativePerformances);
        }
        LOG.debug("Retrieved job information contains {} alternatives and {} performances!", alternatives.getDescriptionOrAlternative().size(),
                performances.getAlternativePerformances().size());

        return wrapMcdaInformation(alternatives, performances, mcdaMethod);
    }

    private McdaInformation getFromCompilation(CompilationJob compilationJob, String mcdaMethod) {
        if (!compilationJob.isReady()) {
            LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
            return null;
        }

        // retrieve required information for the alternatives and performances
        Alternatives alternatives = new Alternatives();
        PerformanceTable performances = new PerformanceTable();
        LOG.debug("Compilation job contains {} results for the ranking!", compilationJob.getJobResults().size());
        for (CompilationResult result : compilationJob.getJobResults()) {

            // add alternative representing the compilation result
            String name = result.getQpu() + "-" + result.getCompiler() + "-" + result.getCircuitName();
            alternatives.getDescriptionOrAlternative().add(createAlternative(result.getId(), name));

            // add performances related to the compilation result
            AlternativeOnCriteriaPerformances alternativePerformances = new AlternativeOnCriteriaPerformances();
            alternativePerformances.setAlternativeID(result.getId().toString());
            List<AlternativeOnCriteriaPerformances.Performance> performanceList = alternativePerformances.getPerformance();
            for (Criterion criterion : xmcdaRepository.findAll()) {

                if (CriteriaConstants.QPU_CRITERION.contains(criterion.getName())) {
                    LOG.debug("Retrieving performance data for criterion {} from QPU!", criterion.getName());
                    performanceList.add(createPerformanceForCircuitCriterion(result, criterion));
                } else if (CriteriaConstants.CIRCUIT_CRITERION.contains(criterion.getName())) {
                    LOG.debug("Retrieving performance data for criterion {} from circuit analysis!", criterion.getName());
                    performanceList.add(createPerformanceForQpuCriterion(criterion));
                } else {
                    LOG.error("Criterion with name {} defined in criteria.xml but retrieval of corresponding data is currently not supported!",
                            criterion.getName());
                }
            }
            performances.getAlternativePerformances().add(alternativePerformances);
        }
        LOG.debug("Retrieved job information contains {} alternatives and {} performances!", alternatives.getDescriptionOrAlternative().size(),
                performances.getAlternativePerformances().size());

        return wrapMcdaInformation(alternatives, performances, mcdaMethod);
    }

    /**
     * Wrap the alternatives and performances in XMCDA and JAXB objects and return a corresponding McdaInformation object
     *
     * @param alternatives the alternatives retrieved from a NISQ Analyzer job
     * @param performances the performances retrieved from a NISQ Analyzer job
     * @return the McdaInformation object containing all required information to invoke the MCDA web services
     */
    private McdaInformation wrapMcdaInformation(Alternatives alternatives, PerformanceTable performances, String mcdaMethod) {
        ObjectFactory objectFactory = new ObjectFactory();

        // create XMCDA wrapper and add criteria
        Criteria criteria = new Criteria();
        criteria.getCriterion().addAll(xmcdaRepository.findAll());
        XMCDA criteriaWrapper = objectFactory.createXMCDA();
        criteriaWrapper.getProjectReferenceOrMethodMessagesOrMethodParameters().add(objectFactory.createXMCDACriteria(criteria));

        // create XMCDA wrapper and add weights
        CriteriaValues criteriaValues = new CriteriaValues();
        criteriaValues.setMcdaConcept("Importance");
        criteriaValues.setName("significance");
        criteriaValues.getCriterionValue().addAll(xmcdaRepository.findValuesByMcdaMethod(mcdaMethod));
        XMCDA weightsWrapper = objectFactory.createXMCDA();
        weightsWrapper.getProjectReferenceOrMethodMessagesOrMethodParameters().add(objectFactory.createXMCDACriteriaValues(criteriaValues));

        // create XMCDA wrapper containing the alternatives and performances required by the MCDA web services
        XMCDA alternativesWrapper = objectFactory.createXMCDA();
        alternativesWrapper.getProjectReferenceOrMethodMessagesOrMethodParameters().add(objectFactory.createXMCDAAlternatives(alternatives));
        XMCDA performancesWrapper = objectFactory.createXMCDA();
        performancesWrapper.getProjectReferenceOrMethodMessagesOrMethodParameters().add(objectFactory.createXMCDAPerformanceTable(performances));

        // return all information to invoke the MCDA services
        McdaInformation mcdaInformation = new McdaInformation();
        mcdaInformation.setCriteria(criteriaWrapper);
        mcdaInformation.setWeights(weightsWrapper);
        mcdaInformation.setAlternatives(alternativesWrapper);
        mcdaInformation.setPerformances(performancesWrapper);
        return mcdaInformation;
    }

    private Alternative createAlternative(UUID id, String name) {
        Alternative alternative = new Alternative();
        alternative.setId(id.toString());
        alternative.setName(name);
        alternative.getDescriptionOrTypeOrActive().add(new JAXBElement(new QName("", "active"), Boolean.class, true));
        alternative.getDescriptionOrTypeOrActive().add(new JAXBElement(new QName("", "type"), String.class, "real"));
        return alternative;
    }

    private AlternativeOnCriteriaPerformances.Performance createPerformanceForCircuitCriterion(QpuSelectionResult result, Criterion criterion) {
        AlternativeOnCriteriaPerformances.Performance performance = new AlternativeOnCriteriaPerformances.Performance();
        performance.setCriterionID(criterion.getId());
        Value value = new Value();
        switch (criterion.getName()) {
            case CriteriaConstants.DEPTH:
                value.setReal((double) result.getAnalyzedDepth());
                break;
            case CriteriaConstants.WIDTH:
                value.setReal((double) result.getAnalyzedWidth());
                break;
            case CriteriaConstants.NUMBER_OF_GATES:
                value.setReal((double) result.getAnalyzedNumberOfGates());
                break;
            case CriteriaConstants.NUMBER_OF_MUTLI_QUBIT_GATES:
                value.setReal((double) result.getAnalyzedNumberOfMultiQubitGates());
                break;
            default:
                LOG.error("Criterion with name {} not supported!", criterion.getName());
        }
        performance.setValue(value);
        return performance;
    }

    private AlternativeOnCriteriaPerformances.Performance createPerformanceForCircuitCriterion(AnalysisResult result, Criterion criterion) {
        AlternativeOnCriteriaPerformances.Performance performance = new AlternativeOnCriteriaPerformances.Performance();
        performance.setCriterionID(criterion.getId());
        Value value = new Value();
        switch (criterion.getName()) {
            case CriteriaConstants.DEPTH:
                value.setReal((double) result.getAnalyzedDepth());
                break;
            case CriteriaConstants.WIDTH:
                value.setReal((double) result.getAnalyzedWidth());
                break;
            case CriteriaConstants.NUMBER_OF_GATES:
                value.setReal((double) result.getAnalyzedNumberOfGates());
                break;
            case CriteriaConstants.NUMBER_OF_MUTLI_QUBIT_GATES:
                value.setReal((double) result.getAnalyzedNumberOfMultiQubitGates());
                break;
            default:
                LOG.error("Criterion with name {} not supported!", criterion.getName());
        }
        performance.setValue(value);
        return performance;
    }

    private AlternativeOnCriteriaPerformances.Performance createPerformanceForCircuitCriterion(CompilationResult result, Criterion criterion) {
        AlternativeOnCriteriaPerformances.Performance performance = new AlternativeOnCriteriaPerformances.Performance();
        performance.setCriterionID(criterion.getId());
        Value value = new Value();
        switch (criterion.getName()) {
            case CriteriaConstants.DEPTH:
                value.setReal((double) result.getAnalyzedDepth());
                break;
            case CriteriaConstants.WIDTH:
                value.setReal((double) result.getAnalyzedWidth());
                break;
            case CriteriaConstants.NUMBER_OF_GATES:
                value.setReal((double) result.getAnalyzedNumberOfGates());
                break;
            case CriteriaConstants.NUMBER_OF_MUTLI_QUBIT_GATES:
                value.setReal((double) result.getAnalyzedNumberOfMultiQubitGates());
                break;
            default:
                LOG.error("Criterion with name {} not supported!", criterion.getName());
        }
        performance.setValue(value);
        return performance;
    }

    private AlternativeOnCriteriaPerformances.Performance createPerformanceForQpuCriterion(Criterion criterion) {
        AlternativeOnCriteriaPerformances.Performance performance = new AlternativeOnCriteriaPerformances.Performance();
        performance.setCriterionID(criterion.getId());
        Value value = new Value();
        switch (criterion.getName()) {
            case CriteriaConstants.QUANTUM_VOLUME:
                // TODO
                break;
            case CriteriaConstants.AVG_CNOT_ERROR:
                // TODO
                break;
            case CriteriaConstants.AVG_READOUT_ERROR:
                // TODO
                break;
            case CriteriaConstants.AVG_T1:
                // TODO
                break;
            default:
                LOG.error("Criterion with name {} not supported!", criterion.getName());
        }
        performance.setValue(value);
        return performance;
    }
}
