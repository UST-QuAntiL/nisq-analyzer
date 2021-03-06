/*******************************************************************************
 * Copyright (c) 2020 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.knowledge.prolog;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Class to update the local Prolog knowledge base depending on changes in the repositories.
 */
@Service
public class PrologFactUpdater {

    final private static Logger LOG = LoggerFactory.getLogger(PrologFactUpdater.class);

    final private static String newline = System.getProperty("line.separator");

    final private PrologKnowledgeBaseHandler prologKnowledgeBaseHandler;

    public PrologFactUpdater(PrologKnowledgeBaseHandler prologKnowledgeBaseHandler) {
        this.prologKnowledgeBaseHandler = prologKnowledgeBaseHandler;
    }

    /**
     * Update the Prolog knowledge base with the required facts for a newly added implementation.
     *
     * @param implementation the added implementation
     */
    public void handleImplementationInsertion(Implementation implementation) {
        LOG.debug("Handling insertion of implementation with Id {} in Prolog knowledge base.", implementation.getId());

        String prologContent = createImplementationFacts(implementation.getId(),
                implementation.getSdk().getName().toLowerCase(),
                implementation.getImplementedAlgorithm(),
                implementation.getSelectionRule());
        try {
            prologKnowledgeBaseHandler.persistPrologFile(prologContent, implementation.getId().toString());
        } catch (IOException e) {
            LOG.error("Unable to store prolog file to add new facts after implementation insertion: {}", e.getMessage());
        }
    }

    /**
     * Update the Prolog knowledge base with the required facts for a newly added SDK connector.
     *
     * @param connector SDK connector to add
     */
    public void handleSDKConnectorInsertion(SdkConnector connector) {
        LOG.debug("Handling insertion of SDK connector with name {} in Prolog knowledge base.", connector.getClass().getSimpleName());

        String prologContent = createSDKConnectorFacts(
                connector.getName(),
                connector.supportedSdks(),
                connector.supportedProviders()
        );
        try {
            prologKnowledgeBaseHandler.persistPrologFile(prologContent, connector.getClass().getSimpleName());
        } catch (IOException e) {
            LOG.error("Unable to store prolog file to add new facts after SDK connector insertion: {}", e.getMessage());
        }
    }

    /**
     * Update the Prolog knowledge base with the required facts for a newly added QPU.
     *
     * @param qpu the added QPU
     */
    public void handleQpuInsertion(Qpu qpu) {
        LOG.debug("Handling insertion of QPU with Id {} in Prolog knowledge base.", qpu.getId());

        String prologContent = createQpuFacts(qpu.getId(),
                qpu.getQubitCount(),
                qpu.getProvider(),
                qpu.getT1(),
                qpu.getMaxGateTime(),
                qpu.isSimulator());
        try {
            prologKnowledgeBaseHandler.persistPrologFile(prologContent, qpu.getId().toString());
        } catch (IOException e) {
            LOG.error("Unable to store prolog file to add new facts after QPU insertion: {}", e.getMessage());
        }
    }

    /**
     * Update the Prolog knowledge base with the required facts for an updated implementation and delete the outdated
     * facts.
     *
     * @param implementation the added implementation
     */
    public void handleImplementationUpdate(Implementation implementation) {
        LOG.debug("Handling update of implementation with Id {} in Prolog knowledge base.", implementation.getId());

        // deactivate and delete the Prolog file with the old facts
        prologKnowledgeBaseHandler.deletePrologFile(implementation.toString());

        // create and activate the Prolog file with the new facts
        String prologContent = createImplementationFacts(implementation.getId(),
                implementation.getSdk().getName().toLowerCase(),
                implementation.getImplementedAlgorithm(),
                implementation.getSelectionRule());
        try {
            prologKnowledgeBaseHandler.persistPrologFile(prologContent, implementation.toString());
        } catch (IOException e) {
            LOG.error("Unable to store prolog file to add new facts after implementation update: {}", e.getMessage());
        }
    }

    /**
     * Update the Prolog knowledge base with the required facts for an updated QPU and delete the outdated facts.
     *
     * @param id            the id of the QPU that is updated in the repository
     * @param qubitCount    the number of provided qubits of the QPU that is updated in the repository
     * @param t1Time        the T1 time for the given QPU
     * @param maxGateTime   the maximum gate time for the given QPU
     * @param supportedSdks the list of supported SDKs of the QPU that is updated in the repository
     */
    public void handleQpuUpdate(UUID id, int qubitCount, List<String> supportedSdks, float t1Time, float maxGateTime, boolean isSimulator) {
        LOG.debug("Handling update of QPU with Id {} in Prolog knowledge base.", id);

        // deactivate and delete the Prolog file with the old facts
        prologKnowledgeBaseHandler.deletePrologFile(id.toString());

        // create and activate the Prolog file with the new facts
        String prologContent = createQpuFacts(id, qubitCount, "", t1Time, maxGateTime, isSimulator);
        try {
            prologKnowledgeBaseHandler.persistPrologFile(prologContent, id.toString());
        } catch (IOException e) {
            LOG.error("Unable to store prolog file to add new facts after QPU update: {}", e.getMessage());
        }
    }

    /**
     * Delete the facts in the knowledge base about the implementation that is deleted from the repository.
     *
     * @param id the id of the implementation that is deleted from the repository
     */
    public void handleImplementationDeletion(Long id) {
        LOG.debug("Handling deletion of implementation with Id {} in Prolog knowledge base.", id);
        prologKnowledgeBaseHandler.deletePrologFile(id.toString());
    }

    /**
     * Delete the facts in the knowledge base about the QPU that is deleted from the repository.
     *
     * @param id the id of the QPU that is deleted from the repository
     */
    public void handleQpuDeletion(Long id) {
        LOG.debug("Handling deletion of QPU with Id {} in Prolog knowledge base.", id);
        prologKnowledgeBaseHandler.deletePrologFile(id.toString());
    }

    /**
     * Create a string containing all required prolog facts for an implementation.
     */
    private String createImplementationFacts(UUID implId, String usedSdk, UUID implementedAlgoId, String selectionRule) {

        String prologContent = "";

        // import prolog packages
        prologContent += ":- use_module(library(regex))." + newline;

        // the following three lines are required to define the same predicate in multiple files
        prologContent += ":- multifile implements/2." + newline;
        prologContent += ":- multifile requiredSdk/2." + newline;
        prologContent += ":- multifile " + getNameOfPredicate(selectionRule) + "/" + PrologUtility.getNumberOfParameters(selectionRule) + "." + newline;

        prologContent += createImplementsFact(implId, implementedAlgoId) + newline;
        prologContent += createRequiredSdkFact(implId, usedSdk) + newline;
        prologContent += selectionRule + newline;

        return prologContent;
    }

    /**
     * Create a string containing all required prolog fact for an QPU.
     */
    private String createQpuFacts(UUID qpuId, int qubitCount, String provider, float t1Time, float maxGateTime, boolean isSimulator) {
        String prologContent = "";

        // import prolog packages
        prologContent += ":- use_module(library(regex))." + newline;

        // the following two lines are required to define the same predicate in multiple files
        prologContent += ":- multifile providesQubits/2." + newline;
        prologContent += ":- multifile usedSdk/2." + newline;
        prologContent += ":- multifile t1Time/2." + newline;
        prologContent += ":- multifile maxGateTime/2." + newline;
        prologContent += ":- multifile hasProvider/2." + newline;
        prologContent += ":- multifile isSimulator/1." + newline;

        prologContent += createProvidesQubitFact(qpuId, qubitCount) + newline;
        prologContent += createHasProviderFact(qpuId, provider) + newline;
        prologContent += createT1TimeFact(qpuId, t1Time) + newline;
        prologContent += createMaxGateTimeFact(qpuId, maxGateTime) + newline;
        prologContent += createIsSimulatorFact(qpuId, isSimulator);
        return prologContent;
    }

    /**
     * Create a list of facts that the given QPU supports the given list of SDKs
     *
     * @param qpuId         the id of the QPU
     * @param supportedSdks the list of SDKs that are supported by the QPU
     * @return the Prolog facts
     */
    private String createUsesSdkFacts(UUID qpuId, List<String> supportedSdks) {
        String prologContent = "";
        for (String supportedSdk : supportedSdks) {
            prologContent += "usedSdk('" + qpuId + "'," + supportedSdk.toLowerCase() + ")." + newline;
        }
        return prologContent;
    }

    /**
     * Create the fact that the given QPU has the given provider
     * @param qpuId the id of the QPU
     * @param provider the provider of the QPU
     * @return the Prolog fact
     */
    private String createHasProviderFact(UUID qpuId, String provider) {
        return "hasProvider('" + qpuId + "','" + provider.toLowerCase() + "').";
    }

    private String createSDKConnectorFacts(String name, List<String> supportedSDKs, List<String> supportedProvider) {

        String prologContent = "";

        // the following lines are required to define the same predicate in multiple files
        prologContent += ":- multifile supportsSDK/2." + newline;
        prologContent += ":- multifile supportsProvider/2." + newline;

        // Add SDKs that are supported by the SDK connector
        for (String sdk : supportedSDKs) {
            prologContent += "supportsSDK(" + name.toLowerCase() + ","  + sdk.toLowerCase() + ")." + newline;
        }

        // Add providers that are supported by the SDK connector
        for (String provider : supportedProvider) {
            prologContent += "supportsProvider(" + name.toLowerCase() + "," + provider.toLowerCase() + ")." + newline;
        }

        return prologContent;
    }

    /**
     * Create a fact whether the given QPU is a simulator
     *
     * @param qpuId         the id of the QPU
     * @param
     * @return the Prolog fact
     */
    private String createIsSimulatorFact(UUID qpuId, boolean isSimulator) {

        if (isSimulator) {
            return "isSimulator('" + qpuId + "')." + newline;
        } else {
            return "";
        }
    }

    /**
     * Create a fact that the given QPU provides the given number of Qubits
     *
     * @param qpuId      the id of the QPU
     * @param qubitCount the number of Qubits that are provided by the QPU
     * @return the Prolog fact
     */
    private String createProvidesQubitFact(UUID qpuId, int qubitCount) {
        return "providesQubits('" + qpuId + "'," + qubitCount + ").";
    }

    /**
     * Create a fact that the given QPU has the given T1 time
     *
     * @param qpuId  the id of the QPU
     * @param t1Time the T1 time for the given QPU
     * @return the Prolog fact
     */
    private String createT1TimeFact(UUID qpuId, float t1Time) {
        return "t1Time('" + qpuId + "'," + t1Time + ").";
    }

    /**
     * Create a fact that the given QPU has the given execution time for the slowest gate
     *
     * @param qpuId       the id of the QPU
     * @param maxGateTime the time of the slowest gate of the QPU
     * @return the Prolog fact
     */
    private String createMaxGateTimeFact(UUID qpuId, float maxGateTime) {
        return "maxGateTime('" + qpuId + "'," + maxGateTime + ").";
    }

    /**
     * Create a fact that the given implementation implements the given algorithm
     *
     * @param implId the id of the implementation
     * @param algoId the id of the algorithm
     * @return the Prolog fact
     */
    private String createImplementsFact(UUID implId, UUID algoId) {
        return "implements('" + implId + "','" + algoId + "').";
    }

    /**
     * Create a fact that the given implementation requires the given SDK
     *
     * @param implId  the id of the implementation
     * @param sdkName the name of the SDK
     * @return the Prolog fact
     */
    private String createRequiredSdkFact(UUID implId, String sdkName) {
        return "requiredSdk('" + implId + "'," + sdkName + ").";
    }

    /**
     * Get the predicate name for the given Prolog rule. E.g. "executable(N, shor-15-qiskit)" --> "executable"
     *
     * @param rule the rule to retrieve the predicate name from
     * @return the name of the predicate
     */
    private String getNameOfPredicate(String rule) {
        return rule.split("\\(")[0];
    }
}
