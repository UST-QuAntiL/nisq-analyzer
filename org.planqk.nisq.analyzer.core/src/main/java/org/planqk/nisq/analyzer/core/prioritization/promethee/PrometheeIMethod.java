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

package org.planqk.nisq.analyzer.core.prioritization.promethee;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

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
import org.xmcda.v2.MethodParameters;
import org.xmcda.v2.ObjectFactory;
import org.xmcda.v2.XMCDA;

import lombok.RequiredArgsConstructor;

/**
 * Service implementing the Promethee I method to prioritize analysis results of the NISQ Analyzer.
 */
@Service
@RequiredArgsConstructor
public class PrometheeIMethod implements McdaMethod {

    private final static Logger LOG = LoggerFactory.getLogger(PrometheeIMethod.class);

    private final JobDataExtractor jobDataExtractor;

    private final McdaJobRepository mcdaJobRepository;

    private final McdaWebServiceHandler mcdaWebServiceHandler;

    private final XmlUtils xmlUtils;

    @Value("${org.planqk.nisq.analyzer.mcda.url}")
    private String baseURL;

    @Override
    public String getName() {
        return "promethee-I";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }

    @Override
    public void executeMcdaMethod(McdaJob mcdaJob) {
        LOG.debug("Starting Promethee I MCDA method to prioritize job with ID: {}", mcdaJob.getJobId());
        McdaInformation mcdaInformation = jobDataExtractor.getJobInformationFromUuid(mcdaJob.getJobId(), mcdaJob.getMethod());

        // abort if job can not be found and therefore no information available
        if (Objects.isNull(mcdaInformation)) {
            LOG.error("Unable to retrieve information about job with ID: {}", mcdaJob.getJobId());
            mcdaJob.setState("failed");
            mcdaJob.setReady(true);
            mcdaJobRepository.save(mcdaJob);
            return;
        }
        try {
            // invoke the preference service for Promothee-I
            LOG.debug("Invoking preference service for Promothee-I!");
            URL url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_PROMOTHEEI_PREFERENCE);
            HashMap<String, String> bodyFields = new HashMap<>();
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_CRITERIA,createVersionedXMCDAString(mcdaInformation.getCriteria()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES,createVersionedXMCDAString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_PERFORMANCES,createVersionedXMCDAString(mcdaInformation.getPerformances()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_WEIGHTS,createVersionedXMCDAString(mcdaInformation.getWeights()));
            Map<String, String>
                    resultsPreferences = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked preference service successfully and retrieved {} results!", resultsPreferences.size());

            // check for required results
            if (!resultsPreferences.containsKey(McdaConstants.WEB_SERVICE_DATA_PREFERENCE)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_PREFERENCE + " in the results but doesn´t! Aborting!");
                return;
            }

            // invoke the flows service for Promothee-I to calculate positive flows
            LOG.debug("Invoking flows service for Promothee-I to calculate positive flows!");
            url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_PROMOTHEEI_FLOWS);
            bodyFields = new HashMap<>();
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_PREFERENCE, resultsPreferences.get(McdaConstants.WEB_SERVICE_DATA_PREFERENCE));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, createVersionedXMCDAString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_FLOW_TYPE, createFlowTypeParameter("POSITIVE"));
            Map<String, String>
                    resultsPositiveFlows = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked flows service successfully and retrieved {} results for positive flows!", resultsPositiveFlows.size());

            // invoke the flows service for Promothee-I to calculate negative flows
            LOG.debug("Invoking flows service for Promothee-I to calculate negative flows!");
            url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_PROMOTHEEI_FLOWS);
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_FLOW_TYPE, createFlowTypeParameter("NEGATIVE"));
            Map<String, String>
                    resultsNegativeFlows = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked flows service successfully and retrieved {} results for negative flows!", resultsNegativeFlows.size());

            // check for required results
            if (!resultsPositiveFlows.containsKey(McdaConstants.WEB_SERVICE_DATA_FLOWS) ||
                    !resultsNegativeFlows.containsKey(McdaConstants.WEB_SERVICE_DATA_FLOWS)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_FLOWS + " in the results but doesn´t! Aborting!");
                return;
            }
            LOG.debug("Resulting negative flows: {}", resultsNegativeFlows.get(McdaConstants.WEB_SERVICE_DATA_FLOWS));

            // TODO: calculate netto flows from results and rank them accordingly
        } catch (MalformedURLException e) {
            setJobToFailed(mcdaJob, "Unable to create URL for invoking the web services!");
        }
    }

    private void setJobToFailed(McdaJob mcdaJob, String errorMessage) {
        LOG.error(errorMessage);
        mcdaJob.setState("failed");
        mcdaJob.setReady(true);
        mcdaJobRepository.save(mcdaJob);
    }

    private String createFlowTypeParameter(String value) {
        ObjectFactory objectFactory = new ObjectFactory();

        MethodParameters methodParameters = new MethodParameters();
        methodParameters.getDescriptionOrApproachOrProblematique()
                .add(new JAXBElement(new QName("", "parameter"), JAXBElement.class,
                        new JAXBElement(new QName("", "value"), JAXBElement.class,
                                new JAXBElement(new QName("", "label"), String.class, value))));

        XMCDA methodParametersWrapper = objectFactory.createXMCDA();
        methodParametersWrapper.getProjectReferenceOrMethodMessagesOrMethodParameters()
                .add(objectFactory.createXMCDAMethodParameters(methodParameters));
        return createVersionedXMCDAString(methodParametersWrapper);
    }

    private String createVersionedXMCDAString(XMCDA xmcda) {
        return xmlUtils.changeXMCDAVersion(xmlUtils.xmcdaToString(xmcda),
                McdaConstants.WEB_SERVICE_NAMESPACE_DEFAULT,
                McdaConstants.WEB_SERVICE_NAMESPACE_2_1_0);
    }
}
