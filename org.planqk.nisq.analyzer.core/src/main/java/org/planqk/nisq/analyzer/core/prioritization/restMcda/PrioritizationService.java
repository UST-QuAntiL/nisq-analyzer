/*******************************************************************************
 * Copyright (c) 2022 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.prioritization.restMcda;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.CircuitResult;
import org.planqk.nisq.analyzer.core.model.CompilationJob;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.JobType;
import org.planqk.nisq.analyzer.core.model.McdaJob;
import org.planqk.nisq.analyzer.core.model.McdaResult;
import org.planqk.nisq.analyzer.core.model.McdaSensitivityAnalysisJob;
import org.planqk.nisq.analyzer.core.model.McdaWeightLearningJob;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.model.xmcda.CriterionValue;
import org.planqk.nisq.analyzer.core.prioritization.JobDataExtractor;
import org.planqk.nisq.analyzer.core.qprov.QProvService;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.CompilationJobRepository;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.McdaJobRepository;
import org.planqk.nisq.analyzer.core.repository.McdaResultRepository;
import org.planqk.nisq.analyzer.core.repository.McdaSensitivityAnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.McdaWeightLearningJobRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionJobRepository;
import org.planqk.nisq.analyzer.core.repository.xmcda.XmcdaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.xmcda.v2.CriteriaValues;
import org.xmcda.v2.Criterion;
import org.xmcda.v2.PerformanceTable;
import org.xmcda.v2.Scale;
import org.xmcda.v2.Value;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrioritizationService {

    private final static Logger LOG = LoggerFactory.getLogger(PrioritizationService.class);

    private final JobDataExtractor jobDataExtractor;

    private final McdaJobRepository mcdaJobRepository;

    private final QpuSelectionJobRepository qpuSelectionJobRepository;

    private final AnalysisJobRepository analysisJobRepository;

    private final CompilationJobRepository compilationJobRepository;

    private final ExecutionResultRepository executionResultRepository;

    private final McdaWeightLearningJobRepository mcdaWeightLearningJobRepository;

    private final McdaSensitivityAnalysisJobRepository mcdaSensitivityAnalysisJobRepository;

    private final McdaResultRepository mcdaResultRepository;

    private final XmcdaRepository xmcdaRepository;

    private final QProvService qProvService;

    @org.springframework.beans.factory.annotation.Value("${org.planqk.nisq.analyzer.prioritization.hostname}")
    private String hostname;

    @org.springframework.beans.factory.annotation.Value("${org.planqk.nisq.analyzer.prioritization.port}")
    private int port;

    @org.springframework.beans.factory.annotation.Value("${org.planqk.nisq.analyzer.prioritization.version}")
    private String version;

    public void executeMcdaMethod(McdaJob mcdaJob) {
        LOG.debug("Starting {} MCDA method to prioritize job with ID: {}", mcdaJob.getMethod(), mcdaJob.getJobId());
        mcdaJob.setState(ExecutionResultStatus.RUNNING.toString());
        mcdaJobRepository.save(mcdaJob);
        PerformanceTable mcdaInformation = jobDataExtractor.getJobInformationFromUuid(mcdaJob);

        // abort if job can not be found and therefore no information available
        if (Objects.isNull(mcdaInformation)) {
            setJobToFailed(mcdaJob, "Unable to retrieve information about job with ID: " + mcdaJob.getJobId());
            return;
        }

        List<McdaCompiledCircuitJob> circuits = new ArrayList<>();
        List<McdaCriteriaPerformances> criteriaPerformancesList = new ArrayList<>();

        mcdaInformation.getAlternativePerformances().forEach(compiledCircuit -> {
            McdaCriteriaPerformances mcdaCriteriaPerformances = new McdaCriteriaPerformances(
                compiledCircuit.getAlternativeID(),
                0.0f,
                compiledCircuit.getPerformance().get(0).getValue().getInteger(),
                compiledCircuit.getPerformance().get(1).getValue().getInteger(),
                compiledCircuit.getPerformance().get(2).getValue().getInteger(),
                compiledCircuit.getPerformance().get(3).getValue().getInteger(),
                compiledCircuit.getPerformance().get(4).getValue().getInteger(),
                compiledCircuit.getPerformance().get(5).getValue().getInteger(),
                compiledCircuit.getPerformance().get(6).getValue().getInteger(),
                compiledCircuit.getPerformance().get(7).getValue().getReal().floatValue(),
                compiledCircuit.getPerformance().get(8).getValue().getReal().floatValue(),
                compiledCircuit.getPerformance().get(9).getValue().getReal().floatValue(),
                compiledCircuit.getPerformance().get(10).getValue().getReal().floatValue(),
                compiledCircuit.getPerformance().get(11).getValue().getReal().floatValue(),
                compiledCircuit.getPerformance().get(12).getValue().getReal().floatValue(),
                compiledCircuit.getPerformance().get(13).getValue().getReal().floatValue(),
                compiledCircuit.getPerformance().get(14).getValue().getInteger());
            criteriaPerformancesList.add(mcdaCriteriaPerformances);
        });
        circuits.add(new McdaCompiledCircuitJob(mcdaJob.getJobId(), criteriaPerformancesList));

        CriteriaValues criteriaValues = new CriteriaValues();
        Map<String, McdaCriterionWeight> metricWeights = new HashMap<>();
        Map<String, McdaCriterionWeight> bordaCountMetrics = new HashMap<>();

        criteriaValues.getCriterionValue().addAll(xmcdaRepository.findValuesByMcdaMethod(mcdaJob.getMethod()));
        criteriaValues.getCriterionValue().forEach(criterionValue -> {
            //get metric weight
            Value value = (Value) criterionValue.getValueOrValues().get(0);
            Optional<Criterion> crit = xmcdaRepository.findById(criterionValue.getCriterionID());
            if (crit.isPresent()) {
                Criterion criterion = crit.get();
                Scale optimum = (Scale) criterion.getActiveOrScaleOrCriterionFunction().get(1);
                LOG.debug("Used weight for metric {} to rank with {}: {}", criterion.getName(), mcdaJob.getMethod(), value.getReal());

                if (mcdaJob.isUseBordaCount() && criterion.getName().equals("queue-size")) {
                    bordaCountMetrics.put(criterion.getName(), new McdaCriterionWeight(0.0f,
                        optimum.getQuantitative().getPreferenceDirection().value().equalsIgnoreCase("min")));
                } else {
                    metricWeights.put(criterion.getName(), new McdaCriterionWeight(value.getReal().floatValue(),
                        optimum.getQuantitative().getPreferenceDirection().value().equalsIgnoreCase("min")));
                }
            }
        });

        String mcdaMethodName = mcdaJob.getMethod();

        if (mcdaMethodName.equals("promethee-II")) {
            mcdaMethodName = "promethee_ii";
        }

        McdaRankRestRequest request = new McdaRankRestRequest(mcdaMethodName, metricWeights, bordaCountMetrics, circuits);

        RestTemplate restTemplate = new RestTemplate();
        try {
            URI resultLocationRedirect =
                restTemplate.postForLocation(URI.create(String.format("http://%s:%d/plugins/es-optimizer@%s/rank", hostname, port, version)),
                    request);

            if (resultLocationRedirect != null) {
                PrioritizationServiceResultLocationResponse prioritizationServiceResultLocationResponse =
                    restTemplate.getForObject(resultLocationRedirect, PrioritizationServiceResultLocationResponse.class);

                while (!prioritizationServiceResultLocationResponse.getLog().equalsIgnoreCase("finished")) {
                    // Wait for next poll
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // pass
                    }
                    prioritizationServiceResultLocationResponse =
                        restTemplate.getForObject(resultLocationRedirect, PrioritizationServiceResultLocationResponse.class);
                }

                try {
                    if (prioritizationServiceResultLocationResponse.getStatus().equalsIgnoreCase("success")) {
                        RankResultResponse rankResultResponse =
                            restTemplate.getForObject(URI.create(prioritizationServiceResultLocationResponse.getOutputs().get(0).getHref()),
                                RankResultResponse.class);

                        List<McdaResult> mcdaResultList = new ArrayList<>();
                        if (mcdaJob.isUseBordaCount()) {
                            rankResultResponse.getScores().forEach((id, score) -> {
                                McdaResult result =
                                    new McdaResult(UUID.fromString(id), rankResultResponse.getBordaCountRanking().indexOf(id) + 1, (double) score);
                                result = mcdaResultRepository.save(result);
                                mcdaResultList.add(result);
                            });
                        } else {
                            rankResultResponse.getScores().forEach((id, score) -> {
                                McdaResult result =
                                    new McdaResult(UUID.fromString(id), rankResultResponse.getRanking().indexOf(id) + 1, (double) score);
                                result = mcdaResultRepository.save(result);
                                mcdaResultList.add(result);
                            });
                        }
                        mcdaJob.setRankedResults(mcdaResultList);
                        mcdaJob.setState(ExecutionResultStatus.FINISHED.toString());
                        mcdaJob.setReady(true);
                        mcdaJobRepository.save(mcdaJob);
                    }
                } catch (RestClientException e) {
                    setJobToFailed(mcdaJob, "Cannot get ranking result from Prioritization Service.");
                }
            }
        } catch (RestClientException e) {
            setJobToFailed(mcdaJob, "Connection to Prioritization Service failed.");
        }
    }

    @Transactional
    public void learnWeights(McdaWeightLearningJob mcdaWeightLearningJob) {
        LOG.debug("Starting {} MCDA method and {} learning method to learn weights", mcdaWeightLearningJob.getMcdaMethod(),
            mcdaWeightLearningJob.getWeightLearningMethod());
        mcdaWeightLearningJob.setState(ExecutionResultStatus.RUNNING.toString());
        mcdaWeightLearningJobRepository.save(mcdaWeightLearningJob);
        List<McdaCompiledCircuitJob> circuits = new ArrayList<>();

        //Fixme: also enable weight learning for Impl-QPU-Selection (Analysis)Results and Compiler-Comparison (Compilation)Results.
        //       Therefore, Histogram Intersection is also required for execution of these Result models. Consider to unify different result models!

        // collect all QpuSelectionJobs with executed Results
        qpuSelectionJobRepository.findAll().forEach(qpuSelectionJob -> {
            McdaCompiledCircuitJob mcdaCompiledCircuitJob = new McdaCompiledCircuitJob();
            if (qpuSelectionJob.isReady() && qpuSelectionJob.getJobResults().size() > 0) {
                mcdaCompiledCircuitJob.setId(qpuSelectionJob.getId());
                List<McdaCriteriaPerformances> compiledCircuits = new ArrayList<>();
                qpuSelectionJob.getJobResults().forEach(qpuSelectionResult -> {
                    List<ExecutionResult> executionResultList = executionResultRepository.findByQpuSelectionResult(qpuSelectionResult);
                    Optional<ExecutionResult> executionResultOptional = executionResultList.stream().filter(
                            exeResult -> exeResult.getShots() > 0 && exeResult.getHistogramIntersectionValue() > 0 &&
                                exeResult.getStatus().equals(ExecutionResultStatus.FINISHED))
                        .findFirst();
                    if (executionResultOptional.isPresent()) {
                        ExecutionResult executionResult = executionResultOptional.get();
                        McdaCriteriaPerformances mcdaCriteriaPerformances = new McdaCriteriaPerformances();
                        mcdaCriteriaPerformances.setId(qpuSelectionResult.getId().toString());
                        mcdaCriteriaPerformances.setHistogramIntersection((float) executionResult.getHistogramIntersectionValue());
                        mcdaCriteriaPerformances.setAnalyzedWidth(qpuSelectionResult.getAnalyzedWidth());
                        mcdaCriteriaPerformances.setAnalyzedDepth(qpuSelectionResult.getAnalyzedDepth());
                        mcdaCriteriaPerformances.setAnalyzedMultiQubitGateDepth(qpuSelectionResult.getAnalyzedMultiQubitGateDepth());
                        mcdaCriteriaPerformances.setAnalyzedTotalNumberOfOperations(qpuSelectionResult.getAnalyzedTotalNumberOfOperations());
                        mcdaCriteriaPerformances.setAnalyzedNumberOfSingleQubitGates(qpuSelectionResult.getAnalyzedNumberOfSingleQubitGates());
                        mcdaCriteriaPerformances.setAnalyzedNumberOfMultiQubitGates(qpuSelectionResult.getAnalyzedNumberOfMultiQubitGates());
                        mcdaCriteriaPerformances.setAnalyzedNumberOfMeasurementOperations(
                            qpuSelectionResult.getAnalyzedNumberOfMeasurementOperations());
                        mcdaCriteriaPerformances.setAvgSingleQubitGateError(qpuSelectionResult.getAvgSingleQubitGateError());
                        mcdaCriteriaPerformances.setAvgMultiQubitGateError(qpuSelectionResult.getAvgMultiQubitGateError());
                        mcdaCriteriaPerformances.setAvgSingleQubitGateTime(qpuSelectionResult.getAvgSingleQubitGateTime());
                        mcdaCriteriaPerformances.setAvgMultiQubitGateTime(qpuSelectionResult.getAvgMultiQubitGateTime());
                        mcdaCriteriaPerformances.setAvgReadoutError(qpuSelectionResult.getAvgReadoutError());
                        mcdaCriteriaPerformances.setT1(qpuSelectionResult.getT1());
                        mcdaCriteriaPerformances.setT2(qpuSelectionResult.getT2());

                        compiledCircuits.add(mcdaCriteriaPerformances);
                    }
                });

                if (compiledCircuits.size() > 0) {
                    mcdaCompiledCircuitJob.setCompiledCircuits(compiledCircuits);
                    circuits.add(mcdaCompiledCircuitJob);
                }
            }
        });

        CriteriaValues criteriaValues = new CriteriaValues();
        Map<String, McdaCriterionWeight> metricWeights = new HashMap<>();

        criteriaValues.getCriterionValue().addAll(xmcdaRepository.findValuesByMcdaMethod(mcdaWeightLearningJob.getMcdaMethod()));
        criteriaValues.getCriterionValue().forEach(criterionValue -> {
            Optional<Criterion> crit = xmcdaRepository.findById(criterionValue.getCriterionID());
            if (crit.isPresent()) {
                Criterion criterion = crit.get();
                Scale optimum = (Scale) criterion.getActiveOrScaleOrCriterionFunction().get(1);

                if (!criterion.getName().equals("queue-size")) {
                    metricWeights.put(criterion.getName(), new McdaCriterionWeight(0.0f,
                        optimum.getQuantitative().getPreferenceDirection().value().equalsIgnoreCase("min")));
                }
            }
        });
        String mcdaMethodName = mcdaWeightLearningJob.getMcdaMethod();
        String weightLearningMethodName = mcdaWeightLearningJob.getWeightLearningMethod();

        if (mcdaMethodName.equals("promethee-II")) {
            mcdaMethodName = "promethee_ii";
        }

        if (weightLearningMethodName.equals("evolution-strategy")) {
            weightLearningMethodName = "es";
        } else if (weightLearningMethodName.equals("genetic-algorithm")) {
            weightLearningMethodName = "ga";
        }

        McdaWeightLearningRequest mcdaWeightLearningRequest = new McdaWeightLearningRequest();
        mcdaWeightLearningRequest.setLearningMethod(weightLearningMethodName);
        mcdaWeightLearningRequest.setMcdaMethod(mcdaMethodName);
        mcdaWeightLearningRequest.setCircuits(circuits);
        LOG.debug("Using {} jobs to learn weights", circuits.size());
        mcdaWeightLearningRequest.setMetricWeights(metricWeights);

        RestTemplate restTemplate = new RestTemplate();
        try {
            URI resultLocationRedirect =
                restTemplate.postForLocation(URI.create(String.format("http://%s:%d/plugins/es-optimizer@%s/learn-ranking", hostname, port, version)),
                    mcdaWeightLearningRequest);

            if (resultLocationRedirect != null) {
                PrioritizationServiceResultLocationResponse prioritizationServiceResultLocationResponse =
                    restTemplate.getForObject(resultLocationRedirect, PrioritizationServiceResultLocationResponse.class);

                while (!prioritizationServiceResultLocationResponse.getLog().equalsIgnoreCase("finished") &&
                    !prioritizationServiceResultLocationResponse.getStatus().equalsIgnoreCase("failure")) {
                    // Wait for next poll
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        // pass
                    }
                    prioritizationServiceResultLocationResponse =
                        restTemplate.getForObject(resultLocationRedirect, PrioritizationServiceResultLocationResponse.class);
                }

                try {
                    if (prioritizationServiceResultLocationResponse.getStatus().equalsIgnoreCase("success")) {
                        ParameterizedTypeReference<HashMap<String, WeightLearningResponse>> responseType =
                            new ParameterizedTypeReference<HashMap<String, WeightLearningResponse>>() {
                            };

                        RequestEntity<Void> request =
                            RequestEntity.get(URI.create(prioritizationServiceResultLocationResponse.getOutputs().get(0).getHref())).build();

                        Map<String, WeightLearningResponse> learnedWeightsResponse = restTemplate.exchange(request, responseType).getBody();

                        learnedWeightsResponse.forEach((String criterion, WeightLearningResponse weight) -> {
                            // find existing entity that should be updated
                            Optional<Criterion> mcdaCriterionOptional =
                                xmcdaRepository.findByCriterionName(criterion);

                            Criterion mcdaCrition = mcdaCriterionOptional.get();

                            Optional<CriterionValue> mcdaCriterionValueOptional =
                                xmcdaRepository.findByCriterionIdAndMethod(mcdaCrition.getId(), mcdaWeightLearningJob.getMcdaMethod());

                            CriterionValue criterionValue = mcdaCriterionValueOptional.get();
                            Value value = (Value) criterionValue.getValueOrValues().get(0);
                            LOG.debug("Previous weight of {} ({}) for {}: {}", criterion, mcdaCrition.getId(), mcdaWeightLearningJob.getMcdaMethod(),
                                value.getReal());
                            value.setReal((double) weight.getNormalizedWeight());
                            LOG.debug("Updated weight of {} ({}) for {} using {}: {}", criterion, mcdaCrition.getId(),
                                mcdaWeightLearningJob.getMcdaMethod(), mcdaWeightLearningJob.getWeightLearningMethod(),
                                value.getReal());
                            xmcdaRepository.updateCriterionValue(criterionValue);
                        });

                        mcdaWeightLearningJob.setState(ExecutionResultStatus.FINISHED.toString());
                        mcdaWeightLearningJob.setReady(true);
                        mcdaWeightLearningJobRepository.save(mcdaWeightLearningJob);
                    } else {
                        mcdaWeightLearningJob.setState(ExecutionResultStatus.FAILED.toString());
                        mcdaWeightLearningJob.setReady(true);
                        mcdaWeightLearningJobRepository.save(mcdaWeightLearningJob);
                    }
                } catch (RestClientException e) {
                    setWeightLearningJobToFailed(mcdaWeightLearningJob, "Cannot get weight learning result from Prioritization Service.");
                }
            }
        } catch (RestClientException e) {
            setWeightLearningJobToFailed(mcdaWeightLearningJob, "Connection to Prioritization Service failed.");
        }
    }

    public void analyzeSensitivity(McdaSensitivityAnalysisJob mcdaSensitivityAnalysisJob) {
        LOG.debug("Using {} MCDA method to analyze sensitivity of job with ID: {}", mcdaSensitivityAnalysisJob.getMethod(),
            mcdaSensitivityAnalysisJob.getJobId());
        mcdaSensitivityAnalysisJob.setState(ExecutionResultStatus.RUNNING.toString());
        mcdaSensitivityAnalysisJobRepository.save(mcdaSensitivityAnalysisJob);

        // get compiled circuits metric values
        List<McdaCriteriaPerformances> compiledCircuits = new ArrayList<>();

        Optional<QpuSelectionJob> qpuSelectionJobOptional = qpuSelectionJobRepository.findById(mcdaSensitivityAnalysisJob.getJobId());
        if (qpuSelectionJobOptional.isPresent()) {
            QpuSelectionJob job = qpuSelectionJobOptional.get();
            if (!job.isReady()) {
                LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
            } else {
                mcdaSensitivityAnalysisJob.setJobType(JobType.QPU_SELECTION);
                mcdaSensitivityAnalysisJobRepository.save(mcdaSensitivityAnalysisJob);
                LOG.debug("Retrieving information from QPU selection job!");
                List<CircuitResult> results = job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
                compiledCircuits = getCircuitResults(results);
            }
        } else {
            LOG.debug("{} is no QpuSelectionJob", mcdaSensitivityAnalysisJob.getJobId());

            Optional<AnalysisJob> analysisJobOptional = analysisJobRepository.findById(mcdaSensitivityAnalysisJob.getJobId());
            if (analysisJobOptional.isPresent()) {
                AnalysisJob job = analysisJobOptional.get();
                if (!job.isReady()) {
                    LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
                } else {
                    mcdaSensitivityAnalysisJob.setJobType(JobType.ANALYSIS);
                    mcdaSensitivityAnalysisJobRepository.save(mcdaSensitivityAnalysisJob);
                    LOG.debug("Retrieving information from analysis job!");
                    List<CircuitResult> results =
                        job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
                    compiledCircuits = getCircuitResults(results);
                }
            } else {
                LOG.debug("{} is no AnalysisJob", mcdaSensitivityAnalysisJob.getJobId());

                Optional<CompilationJob> compilationJobOptional = compilationJobRepository.findById(mcdaSensitivityAnalysisJob.getJobId());
                if (compilationJobOptional.isPresent()) {
                    CompilationJob job = compilationJobOptional.get();
                    if (!job.isReady()) {
                        LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
                    } else {
                        mcdaSensitivityAnalysisJob.setJobType(JobType.COMPILATION);
                        mcdaSensitivityAnalysisJobRepository.save(mcdaSensitivityAnalysisJob);
                        LOG.debug("Retrieving information from compilation job!");
                        List<CircuitResult> results =
                            job.getJobResults().stream().map(jobResult -> (CircuitResult) jobResult).collect(Collectors.toList());
                        compiledCircuits = getCircuitResults(results);
                    }
                } else {
                    LOG.debug("{} is no CompilationJob", mcdaSensitivityAnalysisJob.getJobId());
                    LOG.error("Unable to find QPU selection, analysis, or compilation job for ID: {}", mcdaSensitivityAnalysisJob.getJobId());
                    setSensitivityAnalysisJobToFailed(mcdaSensitivityAnalysisJob,
                        "Unable to retrieve information about job with ID: " + mcdaSensitivityAnalysisJob.getJobId());
                }
            }
        }

        List<McdaCompiledCircuitJob> circuits = new ArrayList<>();
        circuits.add(new McdaCompiledCircuitJob(mcdaSensitivityAnalysisJob.getJobId(), compiledCircuits));

        // get metrics and their weights
        CriteriaValues criteriaValues = new CriteriaValues();
        Map<String, McdaCriterionWeight> metricWeights = new HashMap<>();
        Map<String, McdaCriterionWeight> bordaCountMetrics = new HashMap<>();

        criteriaValues.getCriterionValue().addAll(xmcdaRepository.findValuesByMcdaMethod(mcdaSensitivityAnalysisJob.getMethod()));
        criteriaValues.getCriterionValue().forEach(criterionValue -> {
            //get metric weight
            Value value = (Value) criterionValue.getValueOrValues().get(0);
            Optional<Criterion> crit = xmcdaRepository.findById(criterionValue.getCriterionID());
            if (crit.isPresent()) {
                Criterion criterion = crit.get();
                Scale optimum = (Scale) criterion.getActiveOrScaleOrCriterionFunction().get(1);
                LOG.debug("Initial weight for metric {} to rank with {}: {}", criterion.getName(), mcdaSensitivityAnalysisJob.getMethod(),
                    value.getReal());

                if (mcdaSensitivityAnalysisJob.isUseBordaCount() && criterion.getName().equals("queue-size")) {
                    bordaCountMetrics.put(criterion.getName(), new McdaCriterionWeight(0.0f,
                        optimum.getQuantitative().getPreferenceDirection().value().equalsIgnoreCase("min")));
                } else {
                    metricWeights.put(criterion.getName(), new McdaCriterionWeight(value.getReal().floatValue(),
                        optimum.getQuantitative().getPreferenceDirection().value().equalsIgnoreCase("min")));
                }
            }
        });

        String mcdaMethodName = mcdaSensitivityAnalysisJob.getMethod();

        if (mcdaMethodName.equals("promethee-II")) {
            mcdaMethodName = "promethee_ii";
        }

        McdaSensitivityAnalysisRestRequest request = new McdaSensitivityAnalysisRestRequest();
        request.setMcdaMethod(mcdaMethodName);
        request.setStepSize(mcdaSensitivityAnalysisJob.getStepSize());
        request.setUpperBound(mcdaSensitivityAnalysisJob.getUpperBound());
        request.setLowerBound(mcdaSensitivityAnalysisJob.getLowerBound());
        request.setMetricWeights(metricWeights);
        request.setBordaCountMetrics(bordaCountMetrics);
        request.setCircuits(circuits);

        RestTemplate restTemplate = new RestTemplate();
        try {
            URI resultLocationRedirect =
                restTemplate.postForLocation(
                    URI.create(String.format("http://%s:%d/plugins/es-optimizer@%s/rank-sensitivity", hostname, port, version)),
                    request);

            if (resultLocationRedirect != null) {
                PrioritizationServiceResultLocationResponse prioritizationServiceResultLocationResponse =
                    restTemplate.getForObject(resultLocationRedirect, PrioritizationServiceResultLocationResponse.class);

                while (!prioritizationServiceResultLocationResponse.getLog().equalsIgnoreCase("finished")) {
                    // Wait for next poll
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        // pass
                    }
                    prioritizationServiceResultLocationResponse =
                        restTemplate.getForObject(resultLocationRedirect, PrioritizationServiceResultLocationResponse.class);
                }

                try {
                    if (prioritizationServiceResultLocationResponse.getStatus().equalsIgnoreCase("success")) {
                        //get location where html plot is stored
                        String plotFileLocation = prioritizationServiceResultLocationResponse.getOutputs().get(1).getHref();
                        mcdaSensitivityAnalysisJob.setPlotFileLocation(plotFileLocation);

                        //get location where sensitivity analysis result is stored
                        SensitivityAnalysisResultResponse sensitivityAnalysisResultResponse =
                            restTemplate.getForObject(URI.create(prioritizationServiceResultLocationResponse.getOutputs().get(0).getHref()),
                                SensitivityAnalysisResultResponse.class);

                        List<McdaResult> mcdaResultList = new ArrayList<>();

                        List<McdaCriteriaPerformances> compiledCircuitsCopy = compiledCircuits;
                        if (mcdaSensitivityAnalysisJob.isUseBordaCount()) {
                            compiledCircuits.forEach(circuit -> {
                                McdaResult result = new McdaResult(UUID.fromString(circuit.getId()),
                                    sensitivityAnalysisResultResponse.getOriginalBordaCountRanking().get(compiledCircuitsCopy.indexOf(circuit)) + 1,
                                    sensitivityAnalysisResultResponse.getOriginalScores().get(compiledCircuitsCopy.indexOf(circuit)));
                                result = mcdaResultRepository.save(result);
                                mcdaResultList.add(result);
                            });
                        } else {
                            compiledCircuits.forEach(circuit -> {
                                McdaResult result = new McdaResult(UUID.fromString(circuit.getId()),
                                    sensitivityAnalysisResultResponse.getOriginalRanking().get(compiledCircuitsCopy.indexOf(circuit)) + 1,
                                    sensitivityAnalysisResultResponse.getOriginalScores().get(compiledCircuitsCopy.indexOf(circuit)));
                                result = mcdaResultRepository.save(result);
                                mcdaResultList.add(result);
                            });
                        }

                        mcdaSensitivityAnalysisJob.setOriginalRanking(mcdaResultList);
                        mcdaSensitivityAnalysisJob.setState(ExecutionResultStatus.FINISHED.toString());
                        mcdaSensitivityAnalysisJob.setReady(true);
                        mcdaSensitivityAnalysisJobRepository.save(mcdaSensitivityAnalysisJob);
                    }
                } catch (RestClientException e) {
                    setSensitivityAnalysisJobToFailed(mcdaSensitivityAnalysisJob,
                        "Cannot get sensitivity analysis result from Prioritization Service.");
                }
            }
        } catch (RestClientException e) {
            setSensitivityAnalysisJobToFailed(mcdaSensitivityAnalysisJob, "Connection to Prioritization Service failed.");
        }
    }

    private void setJobToFailed(McdaJob mcdaJob, String errorMessage) {
        LOG.error(errorMessage);
        mcdaJob.setState(ExecutionResultStatus.FAILED.toString());
        mcdaJob.setReady(true);
        mcdaJobRepository.save(mcdaJob);
    }

    private void setWeightLearningJobToFailed(McdaWeightLearningJob mcdaWeightLearningJob, String errorMessage) {
        LOG.error(errorMessage);
        mcdaWeightLearningJob.setState(ExecutionResultStatus.FAILED.toString());
        mcdaWeightLearningJob.setReady(true);
        mcdaWeightLearningJobRepository.save(mcdaWeightLearningJob);
    }

    private void setSensitivityAnalysisJobToFailed(McdaSensitivityAnalysisJob mcdaSensitivityAnalysisJob, String errorMessage) {
        LOG.error(errorMessage);
        mcdaSensitivityAnalysisJob.setState(ExecutionResultStatus.FAILED.toString());
        mcdaSensitivityAnalysisJob.setReady(true);
        mcdaSensitivityAnalysisJobRepository.save(mcdaSensitivityAnalysisJob);
    }

    private List<McdaCriteriaPerformances> getCircuitResults(List<CircuitResult> results) {
        List<McdaCriteriaPerformances> mcdaCriteriaPerformancesList = new ArrayList<>();

        results.forEach(result -> {
            // get QPU object containing required performances data
            Optional<Qpu> qpuOptional = qProvService.getQpuByName(result.getQpu(), result.getProvider());
            if (qpuOptional.isPresent()) {
                McdaCriteriaPerformances mcdaCriteriaPerformances = new McdaCriteriaPerformances();
                mcdaCriteriaPerformances.setId(result.getId().toString());
                mcdaCriteriaPerformances.setHistogramIntersection(0.0f);
                mcdaCriteriaPerformances.setAnalyzedWidth(result.getAnalyzedWidth());
                mcdaCriteriaPerformances.setAnalyzedDepth(result.getAnalyzedDepth());
                mcdaCriteriaPerformances.setAnalyzedMultiQubitGateDepth(result.getAnalyzedMultiQubitGateDepth());
                mcdaCriteriaPerformances.setAnalyzedTotalNumberOfOperations(result.getAnalyzedTotalNumberOfOperations());
                mcdaCriteriaPerformances.setAnalyzedNumberOfSingleQubitGates(result.getAnalyzedNumberOfSingleQubitGates());
                mcdaCriteriaPerformances.setAnalyzedNumberOfMultiQubitGates(result.getAnalyzedNumberOfMultiQubitGates());
                mcdaCriteriaPerformances.setAnalyzedNumberOfMeasurementOperations(
                    result.getAnalyzedNumberOfMeasurementOperations());
                mcdaCriteriaPerformances.setAvgSingleQubitGateError(result.getAvgSingleQubitGateError());
                mcdaCriteriaPerformances.setAvgMultiQubitGateError(result.getAvgMultiQubitGateError());
                mcdaCriteriaPerformances.setAvgSingleQubitGateTime(result.getAvgSingleQubitGateTime());
                mcdaCriteriaPerformances.setAvgMultiQubitGateTime(result.getAvgMultiQubitGateTime());
                mcdaCriteriaPerformances.setAvgReadoutError(result.getAvgReadoutError());
                mcdaCriteriaPerformances.setT1(result.getT1());
                mcdaCriteriaPerformances.setT2(result.getT2());
                mcdaCriteriaPerformances.setQueueSize(qpuOptional.get().getQueueSize());

                mcdaCriteriaPerformancesList.add(mcdaCriteriaPerformances);
            } else {
                LOG.error("Unable to retrieve QPU with name {} at provider {}. Skipping result with ID: {}", result.getQpu(), result.getProvider(),
                    result.getId());
            }
        });
        return mcdaCriteriaPerformancesList;
    }
}
