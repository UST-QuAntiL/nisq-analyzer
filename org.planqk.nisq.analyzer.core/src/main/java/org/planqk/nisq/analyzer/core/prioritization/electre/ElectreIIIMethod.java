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

package org.planqk.nisq.analyzer.core.prioritization.electre;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.planqk.nisq.analyzer.core.model.DynamicXmlElement;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.McdaJob;
import org.planqk.nisq.analyzer.core.model.McdaResult;
import org.planqk.nisq.analyzer.core.prioritization.JobDataExtractor;
import org.planqk.nisq.analyzer.core.prioritization.McdaConstants;
import org.planqk.nisq.analyzer.core.prioritization.McdaInformation;
import org.planqk.nisq.analyzer.core.prioritization.McdaMethod;
import org.planqk.nisq.analyzer.core.prioritization.McdaWebServiceHandler;
import org.planqk.nisq.analyzer.core.prioritization.XmlUtils;
import org.planqk.nisq.analyzer.core.repository.McdaJobRepository;
import org.planqk.nisq.analyzer.core.repository.McdaResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xmcda.v2.Alternative;
import org.xmcda.v2.Alternatives;
import org.xmcda.v2.AlternativesComparisons;
import org.xmcda.v2.MethodParameters;
import org.xmcda.v2.ObjectFactory;
import org.xmcda.v2.XMCDA;

import lombok.RequiredArgsConstructor;

/**
 * Service implementing the Electre III method to prioritize analysis results of the NISQ Analyzer.
 */
@Service
@RequiredArgsConstructor
public class ElectreIIIMethod implements McdaMethod {

    private final static Logger LOG = LoggerFactory.getLogger(ElectreIIIMethod.class);

    private final JobDataExtractor jobDataExtractor;

    private final McdaJobRepository mcdaJobRepository;

    private final McdaResultRepository mcdaResultRepository;

    private final McdaWebServiceHandler mcdaWebServiceHandler;

    private final XmlUtils xmlUtils;

    @Value("${org.planqk.nisq.analyzer.mcda.url}")
    private String baseURL;

    @Override
    public String getName() {
        return "electre-III";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }

    @Override
    public void executeMcdaMethod(McdaJob mcdaJob) {
        LOG.debug("Starting Electre III MCDA method to prioritize job with ID: {}", mcdaJob.getJobId());
        McdaInformation mcdaInformation = jobDataExtractor.getJobInformationFromUuid(mcdaJob);

        // abort if job can not be found and therefore no information available
        if (Objects.isNull(mcdaInformation)) {
            LOG.error("Unable to retrieve information about job with ID: {}", mcdaJob.getJobId());
            mcdaJob.setState(ExecutionResultStatus.FAILED.toString());
            mcdaJob.setReady(true);
            mcdaJobRepository.save(mcdaJob);
            return;
        }

        try {
            // invoke the discordance service for Electre III
            LOG.debug("Invoking discordance service for Electre III!");
            URL url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_ELECTREIII_DISCORDANCE);
            HashMap<String, String> bodyFields = new HashMap<>();
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_CRITERIA, createVersionedXMCDAString(mcdaInformation.getCriteria()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, createVersionedXMCDAString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_PERFORMANCES, createVersionedXMCDAString(mcdaInformation.getPerformances()));
            Map<String, String>
                    resultsDiscordance = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked discordance service successfully and retrieved {} results!", resultsDiscordance.size());

            // check for required results
            if (!resultsDiscordance.containsKey(McdaConstants.WEB_SERVICE_DATA_DISCORDANCES)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_DISCORDANCES + " in the results but doesn´t! Aborting!");
                return;
            }

            // invoke the concordance service for Electre III
            LOG.debug("Invoking concordance service for Electre III!");
            url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_ELECTREIII_CONCORDANCE);
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_WEIGHTS, createVersionedXMCDAString(mcdaInformation.getWeights()));
            Map<String, String>
                    resultsConcordance = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked concordance service successfully and retrieved {} results!", resultsConcordance.size());

            // check for required results
            if (!resultsConcordance.containsKey(McdaConstants.WEB_SERVICE_DATA_CONCORDANCE)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_CONCORDANCE + " in the results but doesn´t! Aborting!");
                return;
            }

            // invoke the outranking service for Electre III
            LOG.debug("Invoking outranking service for Electre III!");
            url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_ELECTREIII_OUTRANKING);
            bodyFields = new HashMap<>();
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_CRITERIA, createVersionedXMCDAString(mcdaInformation.getCriteria()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, createVersionedXMCDAString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_CONCORDANCE, resultsConcordance.get(McdaConstants.WEB_SERVICE_DATA_CONCORDANCE));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_DISCORDANCES, resultsDiscordance.get(McdaConstants.WEB_SERVICE_DATA_DISCORDANCES));
            Map<String, String>
                    resultsOutranking = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked outranking service successfully and retrieved {} results!", resultsOutranking.size());

            // check for required results
            if (!resultsOutranking.containsKey(McdaConstants.WEB_SERVICE_DATA_OUTRANKING)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_OUTRANKING + " in the results but doesn´t! Aborting!");
                return;
            }

            // invoke the cut relation service for Electre III
            LOG.debug("Invoking cut relation service for Electre III!");
            url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_ELECTREIII_RELATION);
            bodyFields = new HashMap<>();
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, createVersionedXMCDAString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_RELATION, resultsOutranking.get(McdaConstants.WEB_SERVICE_DATA_OUTRANKING));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_OPTIONS, createOptionsParameter("classical", "0.5", "classical_binary"));
            Map<String, String>
                    resultsCut = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked cut relation successfully and retrieved {} results!", resultsCut.size());

            // check for required results
            if (!resultsCut.containsKey(McdaConstants.WEB_SERVICE_DATA_OUTPUT_RELATION)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_OUTPUT_RELATION + " in the results but doesn´t! Aborting!");
                return;
            }

            // invoke the ranking service for Electre III
            LOG.debug("Invoking ranking service for Electre III!");
            url = new URL((baseURL.endsWith("/") ? baseURL : baseURL + "/") + McdaConstants.WEB_SERVICE_NAME_ELECTREIII_RANKING);
            bodyFields = new HashMap<>();
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_ALTERNATIVES, createVersionedXMCDAString(mcdaInformation.getAlternatives()));
            bodyFields.put(McdaConstants.WEB_SERVICE_DATA_OUTRANKING_RELATION, resultsCut.get(McdaConstants.WEB_SERVICE_DATA_OUTPUT_RELATION));
            Map<String, String>
                    resultsRanking = mcdaWebServiceHandler.invokeMcdaOperation(url, McdaConstants.WEB_SERVICE_OPERATIONS_INVOKE, bodyFields);
            LOG.debug("Invoked ranking successfully and retrieved {} results!", resultsRanking.size());

            // check for required results
            if (!resultsRanking.containsKey(McdaConstants.WEB_SERVICE_DATA_INTERSECTION_DISTILLATION)) {
                setJobToFailed(mcdaJob,
                        "Invocation must contain " + McdaConstants.WEB_SERVICE_DATA_INTERSECTION_DISTILLATION +
                                " in the results but doesn´t! Aborting!");
                return;
            }

            // rank results according to the received intersection distillation matrix
            String intersectionDistillationString =
                    xmlUtils.changeXMCDAVersion(resultsRanking.get(McdaConstants.WEB_SERVICE_DATA_INTERSECTION_DISTILLATION),
                            McdaConstants.WEB_SERVICE_NAMESPACE_2_0_0,
                            McdaConstants.WEB_SERVICE_NAMESPACE_DEFAULT);
            List<UUID> alternativeIDs = getAlternativeIDs(mcdaInformation.getAlternatives());
            List<McdaResult> results = interpretElectreResults(xmlUtils.stringToXmcda(intersectionDistillationString), alternativeIDs);

            // update job object with results
            if (Objects.isNull(results)) {
                setJobToFailed(mcdaJob, "Unable to rank results by given intersection distillation matrix!");
                return;
            }
            mcdaJob.setRankedResults(results);
            mcdaJob.setState(ExecutionResultStatus.FINISHED.toString());
            mcdaJob.setReady(true);
            mcdaJobRepository.save(mcdaJob);
        } catch (MalformedURLException e) {
            setJobToFailed(mcdaJob, "Unable to create URL for invoking the web services!");
        }
    }

    private List<UUID> getAlternativeIDs(XMCDA xmcda) {
        if (xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters().size() != 1) {
            LOG.error("XMCDA document must contain exactly one root element of type Alternatives!");
            return null;
        }

        // get the root element of the document and check for required type
        Object rootElement = xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters().get(0).getValue();
        if (!(rootElement instanceof Alternatives)) {
            LOG.error("XMCDA document must contain exactly one root element of type Alternatives!");
            return null;
        }

        // extract the IDs of the alternatives
        Alternatives alternatives = (Alternatives) rootElement;
        return alternatives.getDescriptionOrAlternative().stream()
                .filter(object -> object instanceof Alternative)
                .map(object -> (Alternative) object)
                .map(Alternative::getId)
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    private void setJobToFailed(McdaJob mcdaJob, String errorMessage) {
        LOG.error(errorMessage);
        mcdaJob.setState(ExecutionResultStatus.FAILED.toString());
        mcdaJob.setReady(true);
        mcdaJobRepository.save(mcdaJob);
    }

    private String createVersionedXMCDAString(XMCDA xmcda) {
        return xmlUtils.changeXMCDAVersion(xmlUtils.xmcdaToString(xmcda),
                McdaConstants.WEB_SERVICE_NAMESPACE_DEFAULT,
                McdaConstants.WEB_SERVICE_NAMESPACE_2_1_0);
    }

    private String createOptionsParameter(String cutTypeValue, String cutThresholdValue, String classicalOutputValue) {
        ObjectFactory objectFactory = new ObjectFactory();
        MethodParameters methodParameters = new MethodParameters();

        // create wrapper for the cut type, cut threshold, and output method parameters not directly supported by the XMCDA library
        methodParameters.getDescriptionOrApproachOrProblematique()
                .add(new JAXBElement<>(McdaConstants.WEB_SERVICE_QNAMES_PARAMETER, DynamicXmlElement.class,
                        getParameterJaxbElement(McdaConstants.WEB_SERVICE_QNAMES_LABEL, cutTypeValue, McdaConstants.WEB_SERVICE_DATA_CUT_TYPE)));
        methodParameters.getDescriptionOrApproachOrProblematique()
                .add(new JAXBElement<>(McdaConstants.WEB_SERVICE_QNAMES_PARAMETER, DynamicXmlElement.class,
                        getParameterJaxbElement(McdaConstants.WEB_SERVICE_QNAMES_REAL, cutThresholdValue,
                                McdaConstants.WEB_SERVICE_DATA_CUT_THRESHOLD)));
        methodParameters.getDescriptionOrApproachOrProblematique()
                .add(new JAXBElement<>(McdaConstants.WEB_SERVICE_QNAMES_PARAMETER, DynamicXmlElement.class,
                        getParameterJaxbElement(McdaConstants.WEB_SERVICE_QNAMES_LABEL, classicalOutputValue,
                                McdaConstants.WEB_SERVICE_DATA_CLASSICAL_OUTPUT)));

        // generate XMCDA wrapper
        XMCDA methodParametersWrapper = objectFactory.createXMCDA();
        methodParametersWrapper.getProjectReferenceOrMethodMessagesOrMethodParameters()
                .add(objectFactory.createXMCDAMethodParameters(methodParameters));
        return createVersionedXMCDAString(methodParametersWrapper);
    }

    /**
     * Interpret the results of the Electre method and return a corresponding ranking
     *
     * @param intersectionDistillationMatrix the XMCDA object containing the results of the Electre web services
     * @param alternativeIDs                 all the IDs of possible alternatives related to this job
     * @return the list of McdaResults containing the final ranking, or null if the XMCDA is invalid
     */
    private List<McdaResult> interpretElectreResults(XMCDA intersectionDistillationMatrix, List<UUID> alternativeIDs) {

        // get the AlternativesComparisons object containing the result matrix that must be interpreted
        AlternativesComparisons alternativesComparisons = getAlternativesComparison(intersectionDistillationMatrix);
        if (Objects.isNull(alternativesComparisons)) {
            LOG.error("Unable to retrieve AlternativesComparisons from resulting XMCDA. Result interpretation not possible!");
            return null;
        }

        // map storing better alternatives for each possible alternative
        Map<UUID, List<UUID>> rankingWrapperMap = new HashMap<>();
        alternativeIDs.forEach(alternativeID -> rankingWrapperMap.put(alternativeID, new ArrayList<>()));

        // iterate through each entry and store the information in the map
        List<AlternativesComparisons.Pairs.Pair> resultPairs = alternativesComparisons.getPairs().getPair();
        LOG.debug("Result matrix contains {} entries for {} overall alternatives!", resultPairs.size(), alternativeIDs.size());
        for (AlternativesComparisons.Pairs.Pair pair : resultPairs) {

            // get source and target IDs of the entry and skip if it is a diagonal element
            UUID targetID = UUID.fromString(pair.getInitial().getAlternativeID());
            UUID sourceID = UUID.fromString(pair.getTerminal().getAlternativeID());
            if (sourceID.equals(targetID)) {
                continue;
            }

            // retrieve the helper objects for the source element of the matrix entry
            List<UUID> sourceWrapper = rankingWrapperMap.get(sourceID);

            // add information that the target element is better suited than the source element
            sourceWrapper.add(targetID);
            LOG.debug("{} better suited than {}!", targetID, sourceID);
        }

        // calculate position depending on the number of better and worse elements
        List<McdaResult> mcdaResults = new ArrayList<>();
        for (Map.Entry<UUID, List<UUID>> rankingWrapper : rankingWrapperMap.entrySet()) {
            int position = rankingWrapper.getValue().size() + 1;
            LOG.debug("Alternative with ID {} has position {}!", rankingWrapper.getKey(), position);

            McdaResult mcdaResult = new McdaResult();
            mcdaResult.setResultId(rankingWrapper.getKey());
            mcdaResult.setPosition(position);
            mcdaResult.setScore(0);
            mcdaResult = mcdaResultRepository.save(mcdaResult);
            mcdaResults.add(mcdaResult);
        }

        return mcdaResults;
    }

    /**
     * Check if the given XMCDA document contains an AlternativesComparisons and return it or otherwise null
     *
     * @param xmcda the XMCDA document to retrieve the AlternativesComparisons from
     * @return the retrieved AlternativesComparisons, or null if not available as root element
     */
    private AlternativesComparisons getAlternativesComparison(XMCDA xmcda) {
        if (xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters().size() != 1) {
            LOG.error("XMCDA document must contain exactly one root element of type AlternativesComparisons!");
            return null;
        }

        // get the root element of the document and check for required type
        Object rootElement = xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters().get(0).getValue();
        if (!(rootElement instanceof AlternativesComparisons)) {
            LOG.error("XMCDA document must contain exactly one root element of type AlternativesComparisons!");
            return null;
        }

        return (AlternativesComparisons) rootElement;
    }

    /**
     * Generate an XML wrapper for method parameter with the given ID and the value wrapped as required by XMCDA as child elements
     *
     * @param comprisingElementTag the name of the XML tag comprising the value
     * @param value                the value to use for the parameter
     * @param id                   the id of the parameter to define
     * @return an XML wrapper for the given parameters with nested elements containing the given value
     */
    private DynamicXmlElement getParameterJaxbElement(QName comprisingElementTag, String value, String id) {
        JAXBElement<String> labelElement = new JAXBElement<>(comprisingElementTag, String.class, value);
        JAXBElement<JAXBElement<String>> valueElement = new JAXBElement(McdaConstants.WEB_SERVICE_QNAMES_VALUE, JAXBElement.class, labelElement);
        DynamicXmlElement parameter = new DynamicXmlElement();
        parameter.addAttribute(McdaConstants.WEB_SERVICE_QNAMES_ID, id);
        parameter.addElement(valueElement);
        return parameter;
    }
}
