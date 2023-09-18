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

import org.planqk.nisq.analyzer.core.connector.qiskit.QiskitSdkConnector;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.CircuitResult;
import org.planqk.nisq.analyzer.core.model.CompilationJob;
import org.planqk.nisq.analyzer.core.model.JobType;
import org.planqk.nisq.analyzer.core.model.McdaJob;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.qprov.QProvService;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.CompilationJobRepository;
import org.planqk.nisq.analyzer.core.repository.McdaJobRepository;
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

    private final McdaJobRepository mcdaJobRepository;

    private final XmcdaRepository xmcdaRepository;

    private final QProvService qProvService;

    private final QiskitSdkConnector qiskitSdkConnector;

    /**
     * Get the required information to run MCDA methods from different kinds of NISQ Analyzer jobs
     *
     * @param mcdaJob the MCDA related to the prioritization
     * @return the retrieved job information
     */
    public <T> T getJobInformationFromUuid(McdaJob mcdaJob) {
        LOG.debug("Retrieving job information about job with ID: {}", mcdaJob.getJobId());

        Optional<QpuSelectionJob> qpuSelectionJobOptional = qpuSelectionJobRepository.findById(mcdaJob.getJobId());
        if (qpuSelectionJobOptional.isPresent()) {
            QpuSelectionJob job = qpuSelectionJobOptional.get();
            if (!job.isReady()) {
                LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
                return null;
            }

            mcdaJob.setJobType(JobType.QPU_SELECTION);
            mcdaJobRepository.save(mcdaJob);
            LOG.debug("Retrieving information from QPU selection job!");
            List<CircuitResult> results = job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
            return getFromCircuitResults(results, mcdaJob.getMethod());
        }

        Optional<AnalysisJob> analysisJobOptional = analysisJobRepository.findById(mcdaJob.getJobId());
        if (analysisJobOptional.isPresent()) {
            AnalysisJob job = analysisJobOptional.get();
            if (!job.isReady()) {
                LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
                return null;
            }

            mcdaJob.setJobType(JobType.ANALYSIS);
            mcdaJobRepository.save(mcdaJob);
            LOG.debug("Retrieving information from analysis job!");
            List<CircuitResult> results = job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
            return getFromCircuitResults(results, mcdaJob.getMethod());
        }

        Optional<CompilationJob> compilationJobOptional = compilationJobRepository.findById(mcdaJob.getJobId());
        if (compilationJobOptional.isPresent()) {
            CompilationJob job = compilationJobOptional.get();
            if (!job.isReady()) {
                LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
                return null;
            }

            mcdaJob.setJobType(JobType.COMPILATION);
            mcdaJobRepository.save(mcdaJob);
            LOG.debug("Retrieving information from compilation job!");
            List<CircuitResult> results = job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
            return getFromCircuitResults(results, mcdaJob.getMethod());
        }

        LOG.error("Unable to find QPU selection, analysis, or compilation job for ID: {}", mcdaJob.getJobId());
        return null;
    }

    private <T> T getFromCircuitResults(List<CircuitResult> circuitResults, String mcdaMethod) {

        // retrieve required information for the alternatives and performances
        Alternatives alternatives = new Alternatives();
        PerformanceTable performances = new PerformanceTable();
        LOG.debug("Analysis job contains {} results for the ranking!", circuitResults.size());

        for (CircuitResult result : circuitResults) {

            int backendQueueSize = qiskitSdkConnector.getQueueSizeOfQpu(result.getQpu());

            // add alternative representing the analysis result
            String name = result.getQpu() + "-" + result.getCompiler() + "-" + result.getCircuitName();
            alternatives.getDescriptionOrAlternative().add(createAlternative(result.getId(), name));

            // add performances related to the analysis result
            AlternativeOnCriteriaPerformances alternativePerformances = new AlternativeOnCriteriaPerformances();
            alternativePerformances.setAlternativeID(result.getId().toString());
            List<AlternativeOnCriteriaPerformances.Performance> performanceList = alternativePerformances.getPerformance();
            for (Criterion criterion : xmcdaRepository.findAll()) {

                if (McdaConstants.CIRCUIT_CRITERION.contains(criterion.getName().toLowerCase())) {
                    LOG.debug("Retrieving performance data for criterion {} from circuit analysis!", criterion.getName());
                    performanceList.add(createPerformanceForCircuitCriterion(result, criterion));
                } else if (McdaConstants.QPU_CRITERION.contains(criterion.getName().toLowerCase())) {
                    LOG.debug("Retrieving performance data for criterion {} from QPU!", criterion.getName());
                    performanceList.add(createPerformanceForQpuCriterion(backendQueueSize, result, criterion));
                } else {
                    LOG.error("Criterion with name {} defined in criteria.xml but retrieval of corresponding data is currently not supported!",
                        criterion.getName());
                }
            }
            performances.getAlternativePerformances().add(alternativePerformances);
        }
        LOG.debug("Retrieved job information contains {} alternatives and {} performances!", alternatives.getDescriptionOrAlternative().size(),
            performances.getAlternativePerformances().size());
        if (mcdaMethod.equals("electre-III")) {
            return (T) wrapMcdaInformation(alternatives, performances, mcdaMethod);
        } else {
            return (T) performances;
        }
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
            case McdaConstants.DEPTH:
                value.setInteger(result.getAnalyzedDepth());
                break;
            case McdaConstants.WIDTH:
                value.setInteger(result.getAnalyzedWidth());
                break;
            case McdaConstants.TOTAL_NUMBER_OF_OPERATIONS:
                value.setInteger(result.getAnalyzedTotalNumberOfOperations());
                break;
            case McdaConstants.NUMBER_OF_SINGLE_QUBIT_GATES:
                value.setInteger(result.getAnalyzedNumberOfSingleQubitGates());
                break;
            case McdaConstants.NUMBER_OF_MULTI_QUBIT_GATES:
                value.setInteger(result.getAnalyzedNumberOfMultiQubitGates());
                break;
            case McdaConstants.NUMBER_OF_MEASUREMENT_OPERATIONS:
                value.setInteger(result.getAnalyzedNumberOfMeasurementOperations());
                break;
            case McdaConstants.MULTI_QUBIT_GATE_DEPTH:
                value.setInteger(result.getAnalyzedMultiQubitGateDepth());
                break;
            default:
                LOG.error("Criterion with name {} not supported!", criterion.getName());
        }
        performance.setValue(value);
        return performance;
    }

    private AlternativeOnCriteriaPerformances.Performance createPerformanceForQpuCriterion(int backendQueueSize, CircuitResult result,
                                                                                           Criterion criterion) {
        AlternativeOnCriteriaPerformances.Performance performance = new AlternativeOnCriteriaPerformances.Performance();
        performance.setCriterionID(criterion.getId());
        Value value = new Value();
        switch (criterion.getName().toLowerCase()) {
            case McdaConstants.AVG_SINGLE_QUBIT_GATE_ERROR:
                value.setReal((double) result.getAvgSingleQubitGateError());
                break;
            case McdaConstants.AVG_MULTI_QUBIT_GATE_ERROR:
                value.setReal((double) result.getAvgMultiQubitGateError());
                break;
            case McdaConstants.AVG_SINGLE_QUBIT_GATE_TIME:
                value.setReal((double) result.getAvgSingleQubitGateTime());
                break;
            case McdaConstants.AVG_MULTI_QUBIT_GATE_TIME:
                value.setReal((double) result.getAvgMultiQubitGateTime());
                break;
            case McdaConstants.AVG_READOUT_ERROR:
                value.setReal((double) result.getAvgReadoutError());
                break;
            case McdaConstants.AVG_T1:
                if (result.isSimulator()) {
                    value.setReal(99999999.0);
                } else {
                    value.setReal((double) result.getT1());
                }
                break;
            case McdaConstants.AVG_T2:
                if (result.isSimulator()) {
                    value.setReal(99999999.0);
                } else {
                    value.setReal((double) result.getT2());
                }
                break;
            case McdaConstants.QUEUE_SIZE:
                value.setInteger(backendQueueSize);
                break;
            default:
                LOG.error("Criterion with name {} not supported!", criterion.getName());
        }
        performance.setValue(value);
        return performance;
    }
}
