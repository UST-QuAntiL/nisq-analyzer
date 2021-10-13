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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.McdaJob;
import org.planqk.nisq.analyzer.core.prioritization.JobDataExtractor;
import org.planqk.nisq.analyzer.core.prioritization.McdaConstants;
import org.planqk.nisq.analyzer.core.prioritization.McdaInformation;
import org.planqk.nisq.analyzer.core.prioritization.McdaMethod;
import org.planqk.nisq.analyzer.core.prioritization.McdaWebServiceHandler;
import org.planqk.nisq.analyzer.core.prioritization.XmlUtils;
import org.planqk.nisq.analyzer.core.repository.McdaJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private final McdaWebServiceHandler mcdaWebServiceHandler;

    private final XmlUtils xmlUtils;

    @Value("${org.planqk.nisq.analyzer.mcda.url}")
    private String baseURL;

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
        McdaInformation mcdaInformation = jobDataExtractor.getJobInformationFromUuid(mcdaJob.getJobId(), mcdaJob.getMethod());

        // abort if job can not be found and therefore no information available
        if (Objects.isNull(mcdaInformation)) {
            setJobToFailed(mcdaJob,"Unable to retrieve information about job with ID: " + mcdaJob.getJobId());
            return;
        }

        try {
            // invoke the normalization and weighting service for TOPSIS
            LOG.debug("Invoking normalization and weighting service for TOPSIS!");
            URL url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_TOPSIS_WEIGHTING);
            HashMap<String, String> bodyFields = new HashMap<>();
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_CRITERIA, xmlUtils.xmcdaToString(mcdaInformation.getCriteria()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, xmlUtils.xmcdaToString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_PERFORMANCE, xmlUtils.xmcdaToString(mcdaInformation.getPerformances()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_WEIGHTS, xmlUtils.xmcdaToString(mcdaInformation.getWeights()));
            Map<String, String> resultsWeighting = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
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
            Map<String, String> resultsAlternatives = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked alternatives calculation service successfully and retrieved {} results!", resultsAlternatives.size());

            // check for required results
            if (!(resultsAlternatives.containsKey(McdaConstants.WEB_SERVICE_DATA_IDEAL_POSITIVE) &&
                    resultsAlternatives.containsKey(McdaConstants.WEB_SERVICE_DATA_IDEAL_NEGATIVE))) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_IDEAL_POSITIVE + " and " + McdaConstants.WEB_SERVICE_DATA_IDEAL_NEGATIVE + " in the results but doesn´t! Aborting!");
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
            Map<String, String> resultsRanking = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked alternatives calculation service successfully and retrieved {} results!", resultsAlternatives.size());

            // check for required results
            if (!resultsRanking.containsKey(McdaConstants.WEB_SERVICE_DATA_SCORES)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_SCORES + " in the results but doesn´t! Aborting!");
                return;
            }

            // TODO: interpret ranking results and add to McdaJob
            LOG.debug("Resulting scores: {}", resultsRanking.get(McdaConstants.WEB_SERVICE_DATA_SCORES));
        } catch (MalformedURLException e) {
            setJobToFailed(mcdaJob, "Unable to create URL for invoking the web services!");
        }
    }

    private void setJobToFailed(McdaJob mcdaJob, String errorMessage) {
        LOG.error(errorMessage);
        mcdaJob.setState(ExecutionResultStatus.FAILED.toString());
        mcdaJob.setReady(true);
        mcdaJobRepository.save(mcdaJob);
    }
}
