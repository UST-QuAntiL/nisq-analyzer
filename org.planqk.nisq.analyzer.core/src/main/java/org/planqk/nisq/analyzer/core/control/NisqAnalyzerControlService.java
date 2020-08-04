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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologFactUpdater;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologKnowledgeBaseHandler;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologQueryEngine;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologUtility;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.HasId;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.QpuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Control service that handles all internal control flow and invokes the required functionality on behalf of the API.
 */
@Service
public class NisqAnalyzerControlService {

    final private static Logger LOG = LoggerFactory.getLogger(NisqAnalyzerControlService.class);

    final private List<SdkConnector> connectorList;

    final private ImplementationRepository implementationRepository;

    final private ExecutionResultRepository executionResultRepository;

    final private QpuRepository qpuRepository;

    final private PrologQueryEngine prologQueryEngine;

    final private PrologKnowledgeBaseHandler prologKnowledgeBaseHandler;

    public NisqAnalyzerControlService(List<SdkConnector> connectorList,
                                      ImplementationRepository implementationRepository,
                                      ExecutionResultRepository executionResultRepository,
                                      QpuRepository qpuRepository,
                                      PrologQueryEngine prologQueryEngine,
                                      PrologKnowledgeBaseHandler prologKnowledgeBaseHandler) {
        this.connectorList = connectorList;
        this.implementationRepository = implementationRepository;
        this.executionResultRepository = executionResultRepository;
        this.qpuRepository = qpuRepository;
        this.prologQueryEngine = prologQueryEngine;
        this.prologKnowledgeBaseHandler = prologKnowledgeBaseHandler;
    }

    /**
     * Execute the given quantum algorithm implementation with the given input parameters and return the corresponding
     * output of the execution.
     *
     * @param implementation  the quantum algorithm implementation that shall be executed
     * @param qpu             the quantum processing unit to execute the implementation
     * @param inputParameters the input parameters for the execution as key/value pairs
     * @return the ExecutionResult to track the current status and store the result
     * @throws RuntimeException is thrown in case the execution of the algorithm implementation fails
     */
    public ExecutionResult executeQuantumAlgorithmImplementation(Implementation implementation, Qpu qpu, Map<String, String> inputParameters) throws RuntimeException {
        LOG.debug("Executing quantum algorithm implementation with Id: {} and name: {}", implementation.getId(), implementation.getName());

        // get suited Sdk connector plugin
        SdkConnector selectedSdkConnector = connectorList.stream()
                .filter(executor -> executor.supportedSdk().equals(implementation.getSdk().getName()))
                .findFirst().orElse(null);
        if (Objects.isNull(selectedSdkConnector)) {
            LOG.error("Unable to find connector plugin for sdk name {}.", implementation.getSdk().getName());
            throw new RuntimeException("Unable to find connector plugin for sdk name " + implementation.getSdk().getName());
        }

        // create a object to store the execution results
        ExecutionResult executionResult =
                executionResultRepository.save(new ExecutionResult(ExecutionResultStatus.INITIALIZED, "Passing execution to executor plugin.", qpu, null, implementation, inputParameters));

        // execute implementation
        new Thread(() -> selectedSdkConnector.executeQuantumAlgorithmImplementation(implementation.getFileLocation(), qpu, inputParameters, executionResult, executionResultRepository)).start();

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
    public Map<Implementation, List<Qpu>> performSelection(Long algorithm, Map<String, String> inputParameters) throws UnsatisfiedLinkError {
        LOG.debug("Performing implementation and QPU selection for algorithm with Id: {}", algorithm);
        Map<Implementation, List<Qpu>> resultPairs = new HashMap<>();
        // rebuild the prolog files, in case the prolog files arent in temp folder
        rebuildPrologFiles();

        // activate the current prolog files
        implementationRepository.findAll().stream().map(HasId::getId).forEach(id -> prologKnowledgeBaseHandler.activatePrologFile(id.toString()));
        qpuRepository.findAll().stream().map(HasId::getId).forEach(id -> prologKnowledgeBaseHandler.activatePrologFile(id.toString()));

        // check all implementation if they can handle the given set of input parameters
        List<Implementation> implementations = implementationRepository.findByImplementedAlgorithm(algorithm);

        LOG.debug("Found {} implementations for the algorithm.", implementations.size());
        List<Implementation> executableImplementations = implementations.stream()
                .filter(implementation -> prologQueryEngine.checkExecutability(implementation.getSelectionRule(), inputParameters))
                .collect(Collectors.toList());
        LOG.debug("{} implementations are executable for the given input parameters after applying the selection rules.", executableImplementations.size());

        // determine all suitable QPUs for the executable implementations
        for (Implementation execImplementation : executableImplementations) {
            LOG.debug("Searching for suitable Qpu for implementation {} (Id: {}) which requires Sdk {}", execImplementation.getName(), execImplementation.getId(), execImplementation.getSdk().getName());

            // estimate the number of required qubits and the circuit depth by using the corresponding rules if set
            int estimatedQubitCount = Objects.isNull(execImplementation.getWidthRule()) ? 0 : prologQueryEngine.checkWidth(execImplementation.getWidthRule(), inputParameters);
            int estimatedCircuitDepth = Objects.isNull(execImplementation.getDepthRule()) ? 0 : prologQueryEngine.checkDepth(execImplementation.getDepthRule(), inputParameters);

            // get all suitable QPUs for the implementation based on the width and depth estimates
            List<UUID> suitableQpuIds = prologQueryEngine.getSuitableQpus(execImplementation.getId(), estimatedQubitCount, estimatedCircuitDepth);
            if (suitableQpuIds.isEmpty()) {
                LOG.debug("Prolog query returns no suited QPUs. Skipping implementation {} for the selection!", execImplementation.getName());
                continue;
            }

            List<Qpu> qpuCandidates = suitableQpuIds.stream()
                    .map(qpuRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get).collect(Collectors.toList());
            LOG.debug("Filtering based on estimates returned {} QPU candidate(s).", qpuCandidates.size());

            // get suited Sdk connector plugin for the Sdk of the implementation
            SdkConnector selectedSdkConnector = connectorList.stream()
                    .filter(executor -> executor.supportedSdk().equals(execImplementation.getSdk().getName()))
                    .findFirst().orElse(null);

            if (Objects.isNull(selectedSdkConnector)) {
                LOG.warn("Unable to find Sdk connector for Sdk: {}. Adding implementation and possibly suited QPUs to the result based on the estimates!", execImplementation.getSdk());
                // TODO: set flag that the selection is based on estimates
                resultPairs.put(execImplementation, qpuCandidates);
                continue;
            }

            List<Qpu> suitableQpus = new ArrayList<>();
            for (Qpu qpu : qpuCandidates) {
                LOG.debug("Checking if QPU {} is suitable for implementation {}.", qpu.getName(), execImplementation.getName());

                // analyze the quantum circuit by utilizing the capabilities of the suited plugin and retrieve important circuit properties
                CircuitInformation circuitInformation = selectedSdkConnector.getCircuitProperties(execImplementation.getFileLocation(), qpu, inputParameters);
                // TODO: check for null

                // skip qpu if the number of required qubits is greater than the provided
                if (circuitInformation.getCircuitWidth() > qpu.getQubitCount()) {
                    LOG.debug("Required qubit number ({}) is greater than provided number ({}). Skipping Qpu.",
                            circuitInformation.getCircuitWidth(), qpu.getQubitCount());
                    continue;
                }

                // skip qpu if the maximum circuit depth is greater than the required circuit depth
                double maxCircuitDepth = Math.floor(qpu.getT1() / qpu.getMaxGateTime());
                if (circuitInformation.getCircuitDepth() > maxCircuitDepth) {
                    LOG.debug("Required circuit depth ({}) is greater than estimated maximum circuit depth ({}). Skipping Qpu.",
                            circuitInformation.getCircuitDepth(), maxCircuitDepth);
                    continue;
                }

                // qpu is suited candidate to execute the implementation
                suitableQpus.add(qpu);
            }

            // add to result map if at least one suitable QPU is available
            if (!suitableQpus.isEmpty()) {
                resultPairs.put(execImplementation, suitableQpus);
            }
        }

        return resultPairs;
    }

    /**
     * Get the required parameters to select implementations for the given algorithm
     *
     * @param algorithm the id of the algorithm to select an implementation for
     * @return the set of required parameters
     */
    public Set<Parameter> getRequiredSelectionParameters(Long algorithm) {
        // add parameters from the algorithm
        Set<Parameter> requiredParameters = new HashSet<>();

        List<Implementation> implementations = implementationRepository.findByImplementedAlgorithm(algorithm);
        LOG.debug("Retrieving required selection parameters based on {} corresponding implementations.", implementations.size());
        for (Implementation impl : implementations) {
            // add parameters from the implementation
            requiredParameters.addAll(impl.getInputParameters());

            // add parameters from rules
            requiredParameters.addAll(PrologUtility.getParametersForRule(impl.getSelectionRule()));
            requiredParameters.addAll(PrologUtility.getParametersForRule(impl.getWidthRule()));
            requiredParameters.addAll(PrologUtility.getParametersForRule(impl.getDepthRule()));
        }

        return requiredParameters;
    }

    /**
     * rebuild the prolog files for the implementations and qpus, if the app crashs or no prolog files are in temp
     * folder.
     */
    private void rebuildPrologFiles() {
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
        if (qpuRepository.findAll().isEmpty()) {
            LOG.debug("No qpus found in database");
        }
        for (Qpu qpu : qpuRepository.findAll()) {
            if (!prologKnowledgeBaseHandler.doesPrologFileExist(qpu.getId().toString())) {
                prologFactUpdater.handleQpuInsertion(qpu);
                LOG.debug("Rebuild prolog file for qpu {}", qpu.getName());
            }
        }
    }
}
