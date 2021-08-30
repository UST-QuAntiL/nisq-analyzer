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
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.CircuitResult;
import org.planqk.nisq.analyzer.core.model.CompilationJob;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.qprov.QProvService;
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
 */
@Service
@RequiredArgsConstructor
public class JobDataExtractor {

    private final static Logger LOG = LoggerFactory.getLogger(JobDataExtractor.class);

    private final QpuSelectionJobRepository qpuSelectionJobRepository;

    private final AnalysisJobRepository analysisJobRepository;

    private final CompilationJobRepository compilationJobRepository;

    private final XmcdaRepository xmcdaRepository;

    private final QProvService qProvService;

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
            QpuSelectionJob job = qpuSelectionJobOptional.get();
            if (!job.isReady()) {
                LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
                return null;
            }

            LOG.debug("Retrieving information from QPU selection job!");
            List<CircuitResult> results = job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
            return getFromCircuitResults(results, mcdaMethod);
        }

        Optional<AnalysisJob> analysisJobOptional = analysisJobRepository.findById(jobId);
        if (analysisJobOptional.isPresent()) {
            AnalysisJob job = analysisJobOptional.get();
            if (!job.isReady()) {
                LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
                return null;
            }

            LOG.debug("Retrieving information from analysis job!");
            List<CircuitResult> results = job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
            return getFromCircuitResults(results, mcdaMethod);
        }

        Optional<CompilationJob> compilationJobOptional = compilationJobRepository.findById(jobId);
        if (compilationJobOptional.isPresent()) {
            CompilationJob job = compilationJobOptional.get();
            if (!job.isReady()) {
                LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
                return null;
            }

            LOG.debug("Retrieving information from compilation job!");
            List<CircuitResult> results = job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
            return getFromCircuitResults(results, mcdaMethod);
        }

        LOG.error("Unable to find QPU selection, analysis, or compilation job for ID: {}", jobId);
        return null;
    }

    private McdaInformation getFromCircuitResults(List<CircuitResult> circuitResults, String mcdaMethod) {

        // retrieve required information for the alternatives and performances
        Alternatives alternatives = new Alternatives();
        PerformanceTable performances = new PerformanceTable();
        LOG.debug("Analysis job contains {} results for the ranking!", circuitResults.size());
        for (CircuitResult result : circuitResults) {

            // get QPU object containing required performances data
            Optional<Qpu> qpuOptional = qProvService.getQpuByName(result.getQpu(), result.getProvider());
            if (!qpuOptional.isPresent()) {
                LOG.error("Unable to retrieve QPU with name {} at provider {}. Skipping result with ID: {}", result.getQpu(), result.getProvider(),
                        result.getId());
                continue;
            }

            // add alternative representing the analysis result
            String name = result.getQpu() + "-" + result.getCompiler() + "-" + result.getCircuitName();
            alternatives.getDescriptionOrAlternative().add(createAlternative(result.getId(), name));

            // add performances related to the analysis result
            AlternativeOnCriteriaPerformances alternativePerformances = new AlternativeOnCriteriaPerformances();
            alternativePerformances.setAlternativeID(result.getId().toString());
            List<AlternativeOnCriteriaPerformances.Performance> performanceList = alternativePerformances.getPerformance();
            for (Criterion criterion : xmcdaRepository.findAll()) {

                if (CriteriaConstants.CIRCUIT_CRITERION.contains(criterion.getName().toLowerCase())) {
                    LOG.debug("Retrieving performance data for criterion {} from circuit analysis!", criterion.getName());
                    performanceList.add(createPerformanceForCircuitCriterion(result, criterion));
                } else if (CriteriaConstants.QPU_CRITERION.contains(criterion.getName().toLowerCase())) {
                    LOG.debug("Retrieving performance data for criterion {} from QPU!", criterion.getName());
                    performanceList.add(createPerformanceForQpuCriterion(qpuOptional.get(), criterion));
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

    private AlternativeOnCriteriaPerformances.Performance createPerformanceForCircuitCriterion(CircuitResult result, Criterion criterion) {
        AlternativeOnCriteriaPerformances.Performance performance = new AlternativeOnCriteriaPerformances.Performance();
        performance.setCriterionID(criterion.getId());
        Value value = new Value();
        switch (criterion.getName().toLowerCase()) {
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

    private AlternativeOnCriteriaPerformances.Performance createPerformanceForQpuCriterion(Qpu qpu, Criterion criterion) {
        AlternativeOnCriteriaPerformances.Performance performance = new AlternativeOnCriteriaPerformances.Performance();
        performance.setCriterionID(criterion.getId());
        Value value = new Value();
        switch (criterion.getName().toLowerCase()) {
            case CriteriaConstants.QUANTUM_VOLUME:
                // TODO
                break;
            case CriteriaConstants.AVG_CNOT_ERROR:
                // TODO
                break;
            case CriteriaConstants.AVG_READOUT_ERROR:
                value.setReal((double) qpu.getAvgReadoutError());
                break;
            case CriteriaConstants.AVG_T1:
                value.setReal((double) qpu.getT1());
                break;
            default:
                LOG.error("Criterion with name {} not supported!", criterion.getName());
        }
        performance.setValue(value);
        return performance;
    }
}
