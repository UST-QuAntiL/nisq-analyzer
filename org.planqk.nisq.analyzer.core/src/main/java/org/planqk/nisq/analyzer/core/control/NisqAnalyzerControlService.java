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

package org.planqk.nisq.analyzer.core.control;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologFactUpdater;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologKnowledgeBaseHandler;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologQueryEngine;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologUtility;
import org.planqk.nisq.analyzer.core.model.AnalysisCandidate;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.HasId;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.qprov.QProvService;
import org.planqk.nisq.analyzer.core.repository.AnalysisResultRepository;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Control service that handles all internal control flow and invokes the required functionality on behalf of the API.
 */
@RequiredArgsConstructor
@Service
public class NisqAnalyzerControlService {

    final private static Logger LOG = LoggerFactory.getLogger(NisqAnalyzerControlService.class);

    final private List<SdkConnector> connectorList;

    final private ImplementationRepository implementationRepository;

    final private AnalysisResultRepository analysisResultRepository;

    final private ExecutionResultRepository executionResultRepository;

    final private PrologQueryEngine prologQueryEngine;

    final private PrologKnowledgeBaseHandler prologKnowledgeBaseHandler;

    final private QProvService qProvService;

    /**
     * Execute the given quantum algorithm implementation with the given input parameters and return the corresponding
     * output of the execution.
     *
     * @param result          the analysis result that shall be executed
     * @param inputParameters the input parameters for the execution as key/value pairs
     * @return the ExecutionResult to track the current status and store the result
     * @throws RuntimeException is thrown in case the execution of the algorithm implementation fails
     */
    public ExecutionResult executeQuantumAlgorithmImplementation(AnalysisResult result, Map<String, ParameterValue> inputParameters) throws RuntimeException {
        final Implementation implementation = result.getImplementation();
        LOG.debug("Executing quantum algorithm implementation with Id: {} and name: {}", implementation.getId(), implementation.getName());

        // get suited Sdk connector plugin
        SdkConnector selectedSdkConnector = connectorList.stream()
                .filter(executor -> executor.getName().equals(result.getSdkConnector()))
                .findFirst().orElse(null);
        if (Objects.isNull(selectedSdkConnector)) {
            LOG.error("Unable to find connector plugin with name {}.", result.getSdkConnector());
            throw new RuntimeException("Unable to find connector plugin with name " + result.getSdkConnector());
        }

        // Retrieve the QPU from QProv
        Optional<Qpu> qpu = qProvService.getQpuByName(result.getQpu(), result.getProvider());
        if (!qpu.isPresent()) {
            LOG.error("Unable to find qpu with name {}.", result.getQpu());
            throw new RuntimeException("Unable to find qpu with name " + result.getQpu());
        }

        // create a object to store the execution results
        ExecutionResult executionResult =
                executionResultRepository.save(new ExecutionResult(ExecutionResultStatus.INITIALIZED,
                        "Passing execution to executor plugin.", result,
                        null, implementation));

        // execute implementation
        new Thread(() -> selectedSdkConnector.executeQuantumAlgorithmImplementation(implementation.getFileLocation(), qpu.get(), inputParameters, executionResult, executionResultRepository)).start();

        return executionResult;
    }

    /**
     * Perform the selection of suitable implementations and corresponding QPUs for the given algorithm and the provided
     * set of input parameters
     *
     * @param algorithm       the id of the algorithm for which an implementation and corresponding QPU should be
     *                        selected
     * @param inputParameters the set of input parameters required for the selection
     * @return a map with all possible implementations and the corresponding list of QPUs that are suitable to execute
     * them
     * @throws UnsatisfiedLinkError Is thrown if the jpl driver is not on the java class path
     */

    public List<AnalysisResult> performSelection(UUID algorithm, Map<String, String> inputParameters) throws UnsatisfiedLinkError {
        LOG.debug("Performing implementation and QPU selection for algorithm with Id: {}", algorithm);
        List<AnalysisResult> analysisResult = new ArrayList<>();

        // check all implementation if they can handle the given set of input parameters
        List<Implementation> implementations = implementationRepository.findByImplementedAlgorithm(algorithm);

        // Update the Prolog files for implementations
        rebuildImplementationPrologFiles();
        // Activate the Prolog files
        implementationRepository.findAll().stream().map(HasId::getId).forEach(id -> prologKnowledgeBaseHandler.activatePrologFile(id.toString()));

        LOG.debug("Found {} implementations for the algorithm.", implementations.size());
        List<Implementation> executableImplementations = implementations.stream()
                .filter(implementation -> parametersAvailable(getRequiredParameters(implementation), inputParameters))
                .filter(implementation -> prologQueryEngine.checkExecutability(implementation.getSelectionRule(), convertToTypedPrologLiterals(inputParameters, implementation)))
                .collect(Collectors.toList());
        LOG.debug("{} implementations are executable for the given input parameters after applying the selection rules.", executableImplementations.size());

        // Iterate over all providers listed in QProv
        for (Provider provider : qProvService.getProviders()) {

            // Get available QPUs
            List<Qpu> qpus = qProvService.getQPUs(provider);

            // Rebuild the Prolog files for the QPU candidates
            rebuildQPUPrologFiles(qpus);

            // Activate the Prolog files
            implementationRepository.findAll().stream().map(HasId::getId).forEach(id -> prologKnowledgeBaseHandler.activatePrologFile(id.toString()));
            qpus.stream().forEach(qpu -> prologKnowledgeBaseHandler.activatePrologFile(qpu.getId().toString()));
            connectorList.stream().map(c -> c.getClass().getSimpleName()).forEach(name -> prologKnowledgeBaseHandler.activatePrologFile(name));

            // determine all suitable QPUs for the executable implementations
            for (Implementation executableImpl : executableImplementations) {
                LOG.debug("Searching for suitable Qpu for implementation {} (Id: {}) which requires Sdk {}", executableImpl.getName(), executableImpl.getId(), executableImpl.getSdk().getName());

                // get all suitable QPUs for the implementation based on the provided SDK
                List<AnalysisCandidate> suitableCandidates = prologQueryEngine.getSuitableCandidates(executableImpl.getId());
                if (suitableCandidates.isEmpty()) {
                    LOG.debug("Prolog query returns no suited QPUs. Skipping implementation {} for the selection!", executableImpl.getName());
                    continue;
                }
                LOG.debug("After Prolog query {} QPU candidate(s) exist.", suitableCandidates.size());

                // Try to infer the type of the parameters for the given implementation
                Map<String, ParameterValue> execInputParameters = ParameterValue.inferTypedParameterValue(executableImpl.getInputParameters(), inputParameters);

                for (AnalysisCandidate candidate : suitableCandidates) {

                    Qpu qpu = qpus.stream().filter(q -> q.getId().equals(candidate.getQpu())).findFirst().orElse(null);

                    if (Objects.isNull(qpu)) {
                        LOG.warn("Unable to find Qpu with UUID: {}.", candidate.getQpu());
                        continue;
                    }

                    // get suited Sdk connector
                    SdkConnector selectedSdkConnector = connectorList.stream()
                            .filter(executor -> executor.getName().equals(candidate.getSdkConnector()))
                            .findFirst().orElse(null);

                    if (Objects.isNull(selectedSdkConnector)) {
                        LOG.warn("Unable to find Sdk connector: {}.", candidate.getSdkConnector());
                        continue;
                    }

                    LOG.debug("Checking if QPU {} is suitable for implementation {}.", qpu.getName(), executableImpl.getName());

                    // analyze the quantum circuit by utilizing the capabilities of the suited plugin and retrieve important circuit properties
                    CircuitInformation circuitInformation = selectedSdkConnector.getCircuitProperties(executableImpl.getFileLocation(), qpu, execInputParameters);

                    // if something unexpected happened
                    if (Objects.isNull(circuitInformation)) {
                        LOG.error("Circuit analysis by compiler unexpectedly failed.");
                        continue;
                    }

                    // skip qpu if some (expected) error occured during transpilation,
                    // e.g. too many qubits required or the input wasn't suitable for the implementation
                    if (!circuitInformation.wasTranspilationSuccessfull()) {
                        LOG.debug("Transpilation of circuit impossible: {}. Skipping Qpu.", circuitInformation.getError());
                        continue;
                    }

                    if (prologQueryEngine.isQpuSuitable(executableImpl.getId(), qpu.getId(), circuitInformation.getCircuitWidth(), circuitInformation.getCircuitDepth())) {

                        // qpu is suited candidate to execute the implementation
                        analysisResult.add(analysisResultRepository.save(new AnalysisResult(
                                algorithm, qpu.getName(), provider.getName(),
                                selectedSdkConnector.getName(), executableImpl, inputParameters, OffsetDateTime.now(),
                                circuitInformation.getCircuitDepth(), circuitInformation.getCircuitWidth())));

                        LOG.debug("QPU {} suitable for implementation {}.", qpu.getName(), executableImpl.getName());
                    } else {
                        LOG.debug("QPU {} not suitable for implementation {}.", qpu.getName(), executableImpl.getName());
                        continue;
                    }
                }
            }
        }
        return analysisResult;
    }

    /**
     * Get the required parameters to select implementations for the given algorithm
     *
     * @param algorithm the id of the algorithm to select an implementation for
     * @return the set of required parameters
     */
    public Set<Parameter> getRequiredSelectionParameters(UUID algorithm) {
        Set<Parameter> requiredParameters = new HashSet<>();
        connectorList.forEach(connector -> requiredParameters.addAll(connector.getSdkSpecificParameters()));
        implementationRepository.findByImplementedAlgorithm(algorithm).forEach(impl -> requiredParameters.addAll(getRequiredParameters(impl)));

        return requiredParameters;
    }

    private void rebuildImplementationPrologFiles() {
        PrologFactUpdater prologFactUpdater = new PrologFactUpdater(prologKnowledgeBaseHandler);
        if (implementationRepository.findAll().isEmpty()) {
            LOG.debug("No implementations found in database");
        }
        for (Implementation impl : implementationRepository.findAll()) {
            if (!prologKnowledgeBaseHandler.doesPrologFileExist(impl.getId().toString())) {
                prologFactUpdater.handleImplementationInsertion(impl);
                LOG.debug("Rebuild prolog file for implementation {}", impl.getName());
            }
        }

        for (SdkConnector connector : connectorList) {

            String connectorName = connector.getClass().getSimpleName();

            if (!prologKnowledgeBaseHandler.doesPrologFileExist(connectorName)) {
                prologFactUpdater.handleSDKConnectorInsertion(connector);
                LOG.debug("Rebuild prolog file for connector {}", connectorName);
            }
        }
    }

    /**
     * rebuild the prolog files for the implementations and qpus, if the app crashs or no prolog files are in temp
     * folder.
     */
    private void rebuildQPUPrologFiles(List<Qpu> qpus) {
        PrologFactUpdater prologFactUpdater = new PrologFactUpdater(prologKnowledgeBaseHandler);

        // Add Prolog files for the provided QPUs
        for (Qpu qpu : qpus) {
            if (!prologKnowledgeBaseHandler.doesPrologFileExist(qpu.getId().toString())) {
                prologFactUpdater.handleQpuInsertion(qpu);
                LOG.debug("Rebuild prolog file for qpu {}", qpu.getName());
            }
        }
    }

    /**
     * Get all required parameters for an implementation
     *
     * @param impl the implementation
     * @return a set with required parameters
     */
    private Set<Parameter> getRequiredParameters(Implementation impl) {
        Set<Parameter> requiredParameters = new HashSet<>();

        // add parameters from the implementation
        requiredParameters.addAll(impl.getInputParameters());

        // add parameters from rules
        requiredParameters.addAll(PrologUtility.getParametersForRule(impl.getSelectionRule(), false));

        return requiredParameters;
    }

    /**
     * Converts the given parameters to typed literals using the provided data type definition in the implementation.
     */
    private Map<String, String> convertToTypedPrologLiterals(Map<String, String> parameters, Implementation implementation) {

        class EntryParameterPair {

            private Map.Entry<String, String> entry;
            private Optional<Parameter> parameter;

            public EntryParameterPair(Map.Entry<String, String> entry, Optional<Parameter> parameter) {
                this.entry = entry;
                this.parameter = parameter;
            }

            public Optional<Parameter> getParameter() {
                return this.parameter;
            }

            public String getKey() {
                return this.entry.getKey();
            }

            public String getValue() {
                return this.entry.getValue();
            }
        }

        return parameters.entrySet().stream().map(
                // map parameters that are defined in the implementation
                e -> new EntryParameterPair(e, implementation.getInputParameters().stream().filter(p -> p.getName().equals(e.getKey())).findFirst())
        ).filter(
                // filter undefined parameters
                e -> e.getParameter().isPresent()
        ).collect(
                // collect the new map of typed Prolog literals
                Collectors.toMap(e -> e.getKey(), e -> e.getParameter().get().getType() == DataType.String ? "'" + e.getValue() + "'" : e.getValue())
        );
    }

    /**
     * Check if all required parameters are contained in the provided parameters
     *
     * @param requiredParameters the set of required parameters
     * @param providedParameters the map with the provided parameters
     * @return <code>true</code> if all required parameters are contained in the provided parameters, <code>false</code>
     * otherwise
     */
    private boolean parametersAvailable(Set<Parameter> requiredParameters, Map<String, ?> providedParameters) {
        return parametersAvailable(requiredParameters, providedParameters.keySet());
    }

    /**
     * Check if all required parameters are contained in the provided parameters
     *
     * @param requiredParameters     the set of required parameters
     * @param providedParameterNames the set with the provided parameters
     * @return <code>true</code> if all required parameters are contained in the provided parameters, <code>false</code>
     * otherwise
     */
    private boolean parametersAvailable(Set<Parameter> requiredParameters, Set<String> providedParameterNames) {
        LOG.debug("Checking if {} required parameters are available in the input map with {} provided parameters!", requiredParameters.size(), providedParameterNames.size());
        return requiredParameters.stream().allMatch(param -> providedParameterNames.contains(param.getName()));
    }
}
