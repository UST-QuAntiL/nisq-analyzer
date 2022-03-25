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

package org.planqk.nisq.analyzer.core.prioritization.topsis;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.McdaJob;
import org.planqk.nisq.analyzer.core.model.McdaResult;
import org.planqk.nisq.analyzer.core.prioritization.JobDataExtractor;
import org.planqk.nisq.analyzer.core.prioritization.McdaMethod;
import org.planqk.nisq.analyzer.core.prioritization.XmlUtils;
import org.planqk.nisq.analyzer.core.prioritization.restMcda.McdaCompiledCircuitJob;
import org.planqk.nisq.analyzer.core.prioritization.restMcda.McdaCriteriaPerformances;
import org.planqk.nisq.analyzer.core.prioritization.restMcda.McdaCriterionWeight;
import org.planqk.nisq.analyzer.core.prioritization.restMcda.McdaRankRestRequest;
import org.planqk.nisq.analyzer.core.repository.McdaJobRepository;
import org.planqk.nisq.analyzer.core.repository.McdaResultRepository;
import org.planqk.nisq.analyzer.core.repository.xmcda.XmcdaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.xmcda.v2.AlternativeValue;
import org.xmcda.v2.CriteriaValues;
import org.xmcda.v2.Criterion;
import org.xmcda.v2.PerformanceTable;
import org.xmcda.v2.Scale;
import org.xmcda.v2.Value;

import lombok.RequiredArgsConstructor;

/**
 * Service implementing the TOPSIS method to prioritize analysis results of the NISQ Analyzer.
 */
@Service
@RequiredArgsConstructor
public class TopsisMethod implements McdaMethod {

    private final static Logger LOG = LoggerFactory.getLogger(TopsisMethod.class);

    private final JobDataExtractor jobDataExtractor;

    private final McdaJobRepository mcdaJobRepository;

    private final McdaResultRepository mcdaResultRepository;

    private final XmcdaRepository xmcdaRepository;

    private final XmlUtils xmlUtils;

    @org.springframework.beans.factory.annotation.Value("${org.planqk.nisq.analyzer.mcda.url}")
    private String baseURL;

    @org.springframework.beans.factory.annotation.Value("${org.planqk.nisq.analyzer.prioritization.hostname}")
    private String hostname;

    @org.springframework.beans.factory.annotation.Value("${org.planqk.nisq.analyzer.prioritization.port}")
    private int port;

    @org.springframework.beans.factory.annotation.Value("${org.planqk.nisq.analyzer.prioritization.version}")
    private String version;

    @Override
    public String getName() {
        return "topsis";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }

    @Override
    public void executeMcdaMethod(McdaJob mcdaJob) {
        LOG.debug("Starting TOPSIS MCDA method to prioritize job with ID: {}", mcdaJob.getJobId());
        PerformanceTable mcdaInformation = jobDataExtractor.getJobInformationFromUuid(mcdaJob);

        // abort if job can not be found and therefore no information available
        if (Objects.isNull(mcdaInformation)) {
            setJobToFailed(mcdaJob, "Unable to retrieve information about job with ID: " + mcdaJob.getJobId());
            return;
        }

        //try {
        List<McdaCompiledCircuitJob> circuits = new ArrayList<>();
        List<McdaCriteriaPerformances> criteriaPerformancesList = new ArrayList<>();

        mcdaInformation.getAlternativePerformances().forEach(compiledCircuit -> {
            McdaCriteriaPerformances mcdaCriteriaPerformances = new McdaCriteriaPerformances(
                compiledCircuit.getAlternativeID(),
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
            Value value = (Value) criterionValue.getValueOrValues().get(0);
            Optional<Criterion> crit = xmcdaRepository.findById(criterionValue.getCriterionID());
            if (crit.isPresent()) {
                Criterion criterion = crit.get();
                Scale optimum = (Scale) criterion.getActiveOrScaleOrCriterionFunction().get(1);

                if (criterion.getName().equals("queue-size")) {
                    bordaCountMetrics.put(criterion.getName(), new McdaCriterionWeight(0.0f,
                        optimum.getQuantitative().getPreferenceDirection().value().equalsIgnoreCase("min")));
                } else {
                    metricWeights.put(criterion.getName(), new McdaCriterionWeight(value.getReal().floatValue(),
                        optimum.getQuantitative().getPreferenceDirection().value().equalsIgnoreCase("min")));
                }
            }
        });

        McdaRankRestRequest request = new McdaRankRestRequest("topsis", metricWeights, bordaCountMetrics, circuits);

        RestTemplate restTemplate = new RestTemplate();
        try {
            URI resultLocation =
                restTemplate.postForLocation(URI.create(String.format("http://%s:%d/plugins/es-optimizer@%s/rank", hostname, port, version)),
                    request);
        } catch (RestClientException e) {
            setJobToFailed(mcdaJob, "Connection to Prioritization Service failed.");
        }

        /*
            // invoke the normalization and weighting service for TOPSIS
            LOG.debug("Invoking normalization and weighting service for TOPSIS!");
            URL url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_TOPSIS_WEIGHTING);
            HashMap<String, String> bodyFields = new HashMap<>();
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_CRITERIA, xmlUtils.xmcdaToString(mcdaInformation.getCriteria()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, xmlUtils.xmcdaToString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_PERFORMANCE, xmlUtils.xmcdaToString(mcdaInformation.getPerformances()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_WEIGHTS, xmlUtils.xmcdaToString(mcdaInformation.getWeights()));
            Map<String, String> resultsWeighting =
                    mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked normalization and weighting service successfully and retrieved {} results!", resultsWeighting.size());

            // check for required results
            if (!resultsWeighting.containsKey(McdaConstants.WEB_SERVICE_DATA_PERFORMANCE)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_PERFORMANCE + " in the results but doesn´t! Aborting!");
                return;
            }

            // invoke alternatives calculation service for TOPSIS
            LOG.debug("Invoking alternatives calculation service for TOPSIS!");
            url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_TOPSIS_ALTERNATIVES);
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_CRITERIA, xmlUtils.xmcdaToString(mcdaInformation.getCriteria()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, xmlUtils.xmcdaToString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_PERFORMANCE, resultsWeighting.get(McdaConstants.WEB_SERVICE_DATA_PERFORMANCE));
            Map<String, String> resultsAlternatives =
                    mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked alternatives calculation service successfully and retrieved {} results!", resultsAlternatives.size());

            // check for required results
            if (!(resultsAlternatives.containsKey(McdaConstants.WEB_SERVICE_DATA_IDEAL_POSITIVE) &&
                    resultsAlternatives.containsKey(McdaConstants.WEB_SERVICE_DATA_IDEAL_NEGATIVE))) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_IDEAL_POSITIVE + " and " +
                                McdaConstants.WEB_SERVICE_DATA_IDEAL_NEGATIVE + " in the results but doesn´t! Aborting!");
                return;
            }

            // invoke ranking service for TOPSIS
            LOG.debug("Invoking ranking service for TOPSIS!");
            url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_TOPSIS_RANKING);
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_CRITERIA, xmlUtils.xmcdaToString(mcdaInformation.getCriteria()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, xmlUtils.xmcdaToString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_PERFORMANCE, resultsWeighting.get(McdaConstants.WEB_SERVICE_DATA_PERFORMANCE));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_IDEAL_POSITIVE, resultsAlternatives.get(McdaConstants.WEB_SERVICE_DATA_IDEAL_POSITIVE));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_IDEAL_NEGATIVE, resultsAlternatives.get(McdaConstants.WEB_SERVICE_DATA_IDEAL_NEGATIVE));
            Map<String, String> resultsRanking =
                    mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked alternatives calculation service successfully and retrieved {} results!", resultsAlternatives.size());

            // check for required results
            if (!resultsRanking.containsKey(McdaConstants.WEB_SERVICE_DATA_SCORES)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_SCORES + " in the results but doesn´t! Aborting!");
                return;
            }

            // get the alternative values from the ranking
            String versionAdaptedXMCDA = xmlUtils.changeXMCDAVersion(resultsRanking.get(McdaConstants.WEB_SERVICE_DATA_SCORES),
                    McdaConstants.WEB_SERVICE_NAMESPACE_2_2_2,
                    McdaConstants.WEB_SERVICE_NAMESPACE_DEFAULT);
            List<AlternativeValue> alternativeValueList = xmlUtils.getAlternativeValues(xmlUtils.stringToXmcda(versionAdaptedXMCDA));
            LOG.debug("Retrieved {} alternatives from ranking!", alternativeValueList.size());

            // parse results to McdaResults and sort them
            List<McdaResult> mcdaResults = sortAlternatives(alternativeValueList);

            // update job object with the results
            mcdaJob.setRankedResults(mcdaResults);
            mcdaJob.setState(ExecutionResultStatus.FINISHED.toString());
            mcdaJob.setReady(true);
            mcdaJobRepository.save(mcdaJob);
        } catch (MalformedURLException e) {
            setJobToFailed(mcdaJob, "Unable to create URL for invoking the web services!");
        }*/
    }

    private List<McdaResult> sortAlternatives(List<AlternativeValue> alternativeValueList) {
        List<McdaResult> mcdaResults = new ArrayList<>();
        for (AlternativeValue alternativeValue : alternativeValueList) {
            McdaResult mcdaResult = new McdaResult();
            mcdaResult.setScore(xmlUtils.getValue(alternativeValue));
            mcdaResult.setResultId(UUID.fromString(alternativeValue.getAlternativeID()));
            mcdaResult = mcdaResultRepository.save(mcdaResult);
            mcdaResults.add(mcdaResult);
        }

        // sort the list using the scores
        Collections.sort(mcdaResults, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));
        for (int i = 0; i < mcdaResults.size(); i++) {
            McdaResult mcdaResult = mcdaResults.get(i);
            mcdaResult.setPosition(i + 1);
            mcdaResultRepository.save(mcdaResult);
        }

        return mcdaResults;
    }

    private void setJobToFailed(McdaJob mcdaJob, String errorMessage) {
        LOG.error(errorMessage);
        mcdaJob.setState(ExecutionResultStatus.FAILED.toString());
        mcdaJob.setReady(true);
        mcdaJobRepository.save(mcdaJob);
    }
}
