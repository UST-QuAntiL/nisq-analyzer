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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.connector.qiskit.QiskitSdkConnector;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologFactUpdater;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologKnowledgeBaseHandler;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologQueryEngine;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologUtility;
import org.planqk.nisq.analyzer.core.model.AnalysisCandidate;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.CompilationJob;
import org.planqk.nisq.analyzer.core.model.CompilationResult;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.HasId;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.AnalysisResultRepository;
import org.planqk.nisq.analyzer.core.repository.CompilationJobRepository;
import org.planqk.nisq.analyzer.core.repository.CompilerAnalysisResultRepository;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionJobRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;
import org.planqk.nisq.analyzer.core.translator.TranslatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

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

    final private CompilerAnalysisResultRepository compilerAnalysisResultRepository;

    final private ExecutionResultRepository executionResultRepository;

    final private PrologQueryEngine prologQueryEngine;

    final private PrologKnowledgeBaseHandler prologKnowledgeBaseHandler;

    final private QiskitSdkConnector qiskitSdkConnector;

    final private TranslatorService translatorService;

    final private CompilationJobRepository compilationJobRepository;

    final private AnalysisJobRepository analysisJobRepository;

    final private QpuSelectionJobRepository qpuSelectionJobRepository;

    final private QpuSelectionResultRepository qpuSelectionResultRepository;

    /**
     * Execute the given quantum algorithm implementation with the given input parameters and return the corresponding output of the execution.
     *
     * @param result          the analysis result that shall be executed
     * @param inputParameters the input parameters for the execution as key/value pairs
     * @return the ExecutionResult to track the current status and store the result
     * @throws RuntimeException is thrown in case the execution of the algorithm implementation fails
     */
    public ExecutionResult executeQuantumAlgorithmImplementation(AnalysisResult result, Map<String, ParameterValue> inputParameters, String refreshToken)
            throws RuntimeException {
        final Implementation implementation = result.getImplementation();
        LOG.debug("Executing quantum algorithm implementation with Id: {} and name: {}", implementation.getId(), implementation.getName());

        // get suited Sdk connector plugin
        SdkConnector selectedSdkConnector = connectorList.stream()
            .filter(executor -> executor.getName().equals(result.getCompiler()))
            .findFirst().orElse(null);
        if (Objects.isNull(selectedSdkConnector)) {
            LOG.error("Unable to find connector plugin with name {}.", result.getCompiler());
            throw new RuntimeException("Unable to find connector plugin with name " + result.getCompiler());
        }

        // Retrieve the QPU from Qiskit-Service
        Optional<Qpu> qpu = qiskitSdkConnector.getQpuByName(result.getQpu(), result.getProvider(), inputParameters.get("token").toString());
        if (!qpu.isPresent()) {
            LOG.error("Unable to find qpu with name {}.", result.getQpu());
            throw new RuntimeException("Unable to find qpu with name " + result.getQpu());
        }

        // create a object to store the execution results
        ExecutionResult executionResult =
            executionResultRepository.save(new ExecutionResult(ExecutionResultStatus.INITIALIZED,
                "Passing execution to executor plugin.", result, null, null,
                null, 0, 0, implementation));

        // execute implementation
        new Thread(() -> selectedSdkConnector
                .executeQuantumAlgorithmImplementation(implementation, qpu.get(), inputParameters, executionResult,
                        executionResultRepository, refreshToken)).start();

        return executionResult;
    }

    /**
     * Execute the compiled circuit from the given compilation result
     *
     * @param result          the compilation result to execute the circuit for
     * @param inputParameters the input parameters for the execution
     * @return the ExecutionResult to track the current status and store the result
     */
    public ExecutionResult executeCompiledQuantumCircuit(CompilationResult result, Map<String, ParameterValue> inputParameters) {

        // get suited Sdk connector plugin
        SdkConnector selectedSdkConnector = connectorList.stream()
                .filter(executor -> executor.supportedSdks().contains(result.getCompiler()))
                .findFirst().orElse(null);
        if (Objects.isNull(selectedSdkConnector)) {
            LOG.error("Unable to find connector plugin with name {}.", result.getCompiler());
            throw new RuntimeException("Unable to find connector plugin with name " + result.getCompiler());
        }

        // create a object to store the execution results
        ExecutionResult executionResult =
            executionResultRepository.save(new ExecutionResult(ExecutionResultStatus.INITIALIZED,
                "Passing execution to executor plugin.", null, result, null,
                null, 0, 0, null));

        // execute implementation
        new Thread(() -> selectedSdkConnector
            .executeTranspiledQuantumCircuit(result.getTranspiledCircuit(), result.getTranspiledLanguage(), result.getProvider(), result.getQpu(),
                inputParameters,
                executionResult, executionResultRepository, null)).start();

        return executionResult;
    }

    /**
     * Execute the compiled circuit from the given qpu-selection-result
     *
     * @param result          the compilation result to execute the circuit for
     * @param inputParameters the input parameters for the execution
     * @return the ExecutionResult to track the current status and store the result
     */
    public ExecutionResult executeCompiledQpuSelectionCircuit(QpuSelectionResult result, Map<String, ParameterValue> inputParameters) {

        // get suited Sdk connector plugin
        SdkConnector selectedSdkConnector = connectorList.stream()
            .filter(executor -> executor.supportedSdks().contains(result.getCompiler()))
            .findFirst().orElse(null);
        if (Objects.isNull(selectedSdkConnector)) {
            LOG.error("Unable to find connector plugin with name {}.", result.getCompiler());
            throw new RuntimeException("Unable to find connector plugin with name " + result.getCompiler());
        }

        // create a object to store the execution results
        ExecutionResult executionResult =
            executionResultRepository.save(new ExecutionResult(ExecutionResultStatus.INITIALIZED,
                "Passing execution to executor plugin.", null, null, result,
                null, 0, 0, null));

        // execute implementation
        new Thread(() -> selectedSdkConnector
            .executeTranspiledQuantumCircuit(result.getTranspiledCircuit(), result.getTranspiledLanguage(), result.getProvider(), result.getQpu(),
                inputParameters,
                executionResult, executionResultRepository, qpuSelectionResultRepository)).start();

        return executionResult;
    }

    /**
     * Perform the selection of suitable implementations and corresponding QPUs for the given algorithm and the provided set of input parameters
     *
     * @param algorithm       the id of the algorithm for which an implementation and corresponding QPU should be selected
     * @param inputParameters the set of input parameters required for the selection
     * @return a map with all possible implementations and the corresponding list of QPUs that are suitable to execute them
     * @throws UnsatisfiedLinkError Is thrown if the jpl driver is not on the java class path
     */

    public void performSelection(AnalysisJob job, UUID algorithm, Map<String, String> inputParameters, String refreshToken)
        throws UnsatisfiedLinkError {
        LOG.debug("Performing implementation and QPU selection for algorithm with Id: {}", algorithm);

        // check all implementation if they can handle the given set of input parameters
        List<Implementation> implementations = implementationRepository.findByImplementedAlgorithm(algorithm);

        // Update the Prolog files for implementations
        rebuildImplementationPrologFiles();
        // Activate the Prolog files
        implementationRepository.findAll().stream().map(HasId::getId).forEach(id -> prologKnowledgeBaseHandler.activatePrologFile(id.toString()));

        LOG.debug("Found {} implementations for the algorithm.", implementations.size());
        List<Implementation> executableImplementations = implementations.stream()
            .filter(implementation -> parametersAvailable(getRequiredParameters(implementation), inputParameters))
            .filter(implementation -> prologQueryEngine
                .checkExecutability(implementation.getSelectionRule(), convertToTypedPrologLiterals(inputParameters, implementation)))
            .collect(Collectors.toList());
        LOG.debug("{} implementations are executable for the given input parameters after applying the selection rules.",
            executableImplementations.size());

        List<AnalysisResult> analysisResults = new ArrayList<>();

        // Iterate over all providers listed in Qiskit Service
        for (Provider provider : qiskitSdkConnector.getProviders()) {

            // Get available QPUs
            List<Qpu> qpus = qiskitSdkConnector.getQPUs(provider, inputParameters.get("token"));

            // Rebuild the Prolog files for the QPU candidates
            rebuildQPUPrologFiles(qpus);

            // Activate the Prolog files
            implementationRepository.findAll().stream().map(HasId::getId).forEach(id -> prologKnowledgeBaseHandler.activatePrologFile(id.toString()));
            qpus.stream().forEach(qpu -> prologKnowledgeBaseHandler.activatePrologFile(qpu.getId().toString()));
            connectorList.stream().map(c -> c.getClass().getSimpleName()).forEach(name -> prologKnowledgeBaseHandler.activatePrologFile(name));

            // determine all suitable QPUs for the executable implementations
            for (Implementation executableImpl : executableImplementations) {
                LOG.debug("Searching for suitable Qpu for implementation {} (Id: {}) which requires Sdk {}", executableImpl.getName(),
                        executableImpl.getId(), executableImpl.getSdk().getName());

                // get all suitable QPUs for the implementation based on the provided SDK
                List<AnalysisCandidate> suitableCandidates = prologQueryEngine.getSuitableCandidates(executableImpl.getId());
                if (suitableCandidates.isEmpty()) {
                    LOG.debug("Prolog query returns no suited QPUs. Skipping implementation {} for the selection!", executableImpl.getName());
                    continue;
                }
                LOG.debug("After Prolog query {} QPU candidate(s) exist.", suitableCandidates.size());

                // Try to infer the type of the parameters for the given implementation
                Map<String, ParameterValue> execInputParameters =
                        ParameterValue.inferTypedParameterValue(executableImpl.getInputParameters(), inputParameters);

                for (AnalysisCandidate candidate : suitableCandidates) {

                    Qpu qpu = qpus.stream().filter(q -> q.getId().equals(candidate.getQpu())).findFirst().orElse(null);

                    if (Objects.isNull(qpu)) {
                        LOG.warn("Unable to find Qpu with UUID: {}.", candidate.getQpu());
                        continue;
                    }

                    // get suited Sdk connector
                    SdkConnector selectedSdkConnector = connectorList.stream()
                            .filter(executor -> executor.getName().equals(candidate.getCompiler()))
                            .findFirst().orElse(null);

                    if (Objects.isNull(selectedSdkConnector)) {
                        LOG.warn("Unable to find Sdk connector: {}.", candidate.getCompiler());
                        continue;
                    }

                    LOG.debug("Checking if QPU {} is suitable for implementation {}.", qpu.getName(), executableImpl.getName());

                    // analyze the quantum circuit by utilizing the capabilities of the suited plugin and retrieve important circuit properties
                    CircuitInformation circuitInformation =
                            selectedSdkConnector.getCircuitProperties(executableImpl, qpu.getProvider(), qpu.getName(), execInputParameters, refreshToken);

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

                    if (prologQueryEngine.isQpuSuitable(executableImpl.getId(), qpu.getId(), circuitInformation.getCircuitWidth(),
                            circuitInformation.getCircuitDepth())) {

                        // qpu is suited candidate to execute the implementation
                        AnalysisResult result = new AnalysisResult();
                        result.setImplementation(executableImpl);
                        result.setImplementedAlgorithm(algorithm);
                        result.setTime(OffsetDateTime.now());
                        result.setInputParameters(inputParameters);
                        result.setQpu(qpu.getName());
                        result.setCircuitName(executableImpl.getName());
                        result.setProvider(provider.getName());
                        result.setCompiler(selectedSdkConnector.getName());
                        result.setAnalyzedDepth(circuitInformation.getCircuitDepth());
                        result.setAnalyzedWidth(circuitInformation.getCircuitWidth());
                        result.setAnalyzedTotalNumberOfOperations(circuitInformation.getCircuitTotalNumberOfOperations());
                        result.setAnalyzedNumberOfSingleQubitGates(circuitInformation.getCircuitNumberOfSingleQubitGates());
                        result.setAnalyzedNumberOfMeasurementOperations(circuitInformation.getCircuitNumberOfMeasurementOperations());
                        result.setAnalyzedNumberOfMultiQubitGates(circuitInformation.getCircuitNumberOfMultiQubitGates());
                        result.setAnalyzedMultiQubitGateDepth(circuitInformation.getCircuitMultiQubitGateDepth());
                        result.setAvgMultiQubitGateError(qpu.getAvgMultiQubitGateError());
                        result.setAvgMultiQubitGateTime(qpu.getAvgMultiQubitGateTime());
                        result.setAvgReadoutError(qpu.getAvgReadoutError());
                        result.setAvgSingleQubitGateError(qpu.getAvgSingleQubitGateError());
                        result.setAvgSingleQubitGateTime(qpu.getAvgSingleQubitGateTime());
                        result.setT1(qpu.getT1());
                        result.setT2(qpu.getT2());
                        result.setMaxGateTime(qpu.getMaxGateTime());
                        result.setQubitCount(qpu.getQubitCount());
                        result.setSimulator(qpu.isSimulator());
                        result = analysisResultRepository.save(result);

                        analysisResults.add(result);
                        job.setJobResults(analysisResults);
                        job = analysisJobRepository.save(job);

                        LOG.debug("QPU {} suitable for implementation {}.", qpu.getName(), executableImpl.getName());
                    } else {
                        LOG.debug("QPU {} not suitable for implementation {}.", qpu.getName(), executableImpl.getName());
                    }
                }
            }
        }

        job.setReady(true);
        analysisJobRepository.save(job);
    }

    public void performSelection(AnalysisJob job, UUID algorithm, Map<String, String> inputParameters) throws UnsatisfiedLinkError {
        performSelection(job, algorithm, inputParameters, "");
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

    /**
     * Compile the given circuit for the given QPU with all supported or a subset of the supported compilers and return the resulting compiled
     * circuits as well as some analysis details
     *
     * @param job             the compilation job object for the long-running task
     * @param providerName    the name of the provider of the QPU
     * @param qpuName         the name of the QPU for which the circuit should be compiled
     * @param circuitLanguage the language of the quantum circuit
     * @param circuitCode     the file containing the circuit to compile
     * @param circuitName     user defined name to (partly) distinguish circuits
     * @param compilerNames   an optional list of compiler names to restrict the compilers to use. If not set, all supported compilers are used
     * @param token           the token to access the specified QPU
     */
    public void performCompilerSelection(CompilationJob job, String providerName, String qpuName, String circuitLanguage,
                                         File circuitCode, String circuitName, List<String> compilerNames, String token) {

        // analyze compilers and retrieve suitable compilation results
        List<CompilationResult> compilerAnalysisResults =
                selectCompiler(providerName, qpuName, circuitLanguage, circuitCode, circuitName, compilerNames, token);

        // add result to DB and connect with CompilationJob
        for (CompilationResult result : compilerAnalysisResults) {
            CompilationResult compilationResult = compilerAnalysisResultRepository.save(result);
            job.getJobResults().add(compilationResult);
        }

        // store updated result object
        LOG.debug("Results: " + job.getJobResults().size());
        job.setReady(true);
        compilationJobRepository.save(job);
    }

    /**
     * Perform the selection of a suitable QPUs for the given quantum circuit
     *
     * @param job               the QPU selection job for the long-running task
     * @param allowedProviders  an optional list with providers to include into the selection. If not specified all providers are taken into account.
     * @param circuitLanguage   the language of the circuit for which the QPU selection should be performed
     * @param circuitCode       the file containing the circuit
     * @param tokens            a map with access tokens for the different quantum hardware providers
     * @param simulatorsAllowed <code>true</code> if also simulators should be included into the selection, <code>false</code> otherwise
     * @param circuitName     user defined name to (partly) distinguish circuits
     */
    public void performQpuSelectionForCircuit(QpuSelectionJob job, List<String> allowedProviders, String circuitLanguage, File circuitCode,
                                              Map<String,String> tokens, boolean simulatorsAllowed, String circuitName, List<String> compilers) {

        // make name of providers case-insensitive
        TreeMap<String, String> caseInsensitiveTokens = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveTokens.putAll(tokens);

        if (circuitName == null) {
            circuitName = "temp";
        }

        // iterate over all providers listed in Qiskit Service for the QPU selection
        for (Provider provider : qiskitSdkConnector.getProviders()) {

            // filter providers that are not contained in the list of allowed providers
            if (Objects.nonNull(allowedProviders) &&
                allowedProviders.stream().noneMatch(allowedProvider -> allowedProvider.equalsIgnoreCase(provider.getName()))) {
                LOG.debug("Provider '{}' is not contained in list of allowed providers. Skipping!", provider.getName());
                continue;
            }

            LOG.debug("Performing QPU selection for provider with name: {}", provider.getName());

            // get available QPUs
            List<Qpu> qpus = qiskitSdkConnector.getQPUs(provider, caseInsensitiveTokens.get(provider.getName()));
            LOG.debug("Found {} QPUs from provider '{}'!", qpus.size(), provider.getName());

            for (Qpu qpu : qpus) {
                LOG.debug("Checking if QPU '{}' is suitable for the execution of the given circuit...", qpu.getName());

                if (!simulatorsAllowed && qpu.isSimulator()) {
                    LOG.debug("Skipping simulator as they are disabled for the selection!");
                    continue;
                }

                String token = caseInsensitiveTokens.get(provider.getName());
                if (Objects.isNull(token)) {
                    LOG.debug("No suited access token for this provider available. Skipping!");
                    continue;
                }

                // perform compiler selection for the given QPU and circuit
                List<CompilationResult> compilationResults =
                        selectCompiler(provider.getName(), qpu.getName(), circuitLanguage, circuitCode, circuitName, compilers, token);
                LOG.debug("Retrieved {} compilation results!", compilationResults.size());

                // add results to the database and the job
                for (CompilationResult result : compilationResults) {
                    QpuSelectionResult qpuSelectionResult = new QpuSelectionResult();
                    qpuSelectionResult.setCircuitName(result.getCircuitName());
                    qpuSelectionResult.setTranspiledCircuit(result.getTranspiledCircuit());
                    qpuSelectionResult.setTranspiledLanguage(result.getTranspiledLanguage());
                    qpuSelectionResult.setTime(OffsetDateTime.now());
                    qpuSelectionResult.setQueueSize(qpu.getQueueSize());
                    qpuSelectionResult.setQpu(qpu.getName());
                    qpuSelectionResult.setProvider(provider.getName());
                    qpuSelectionResult.setCompiler(result.getCompiler());
                    qpuSelectionResult.setAnalyzedDepth(result.getAnalyzedDepth());
                    qpuSelectionResult.setAnalyzedWidth(result.getAnalyzedWidth());
                    qpuSelectionResult.setAnalyzedTotalNumberOfOperations(result.getAnalyzedTotalNumberOfOperations());
                    qpuSelectionResult.setAnalyzedNumberOfSingleQubitGates(result.getAnalyzedNumberOfSingleQubitGates());
                    qpuSelectionResult.setAnalyzedNumberOfMeasurementOperations(result.getAnalyzedNumberOfMeasurementOperations());
                    qpuSelectionResult.setAnalyzedNumberOfMultiQubitGates(result.getAnalyzedNumberOfMultiQubitGates());
                    qpuSelectionResult.setAnalyzedMultiQubitGateDepth(result.getAnalyzedMultiQubitGateDepth());
                    qpuSelectionResult.setToken(result.getToken());
                    qpuSelectionResult.setQpuSelectionJobId(job.getId());
                    qpuSelectionResult.setAvgMultiQubitGateError(qpu.getAvgMultiQubitGateError());
                    qpuSelectionResult.setAvgMultiQubitGateTime(qpu.getAvgMultiQubitGateTime());
                    qpuSelectionResult.setAvgReadoutError(qpu.getAvgReadoutError());
                    qpuSelectionResult.setAvgSingleQubitGateError(qpu.getAvgSingleQubitGateError());
                    qpuSelectionResult.setAvgSingleQubitGateTime(qpu.getAvgSingleQubitGateTime());
                    qpuSelectionResult.setT1(qpu.getT1());
                    qpuSelectionResult.setT2(qpu.getT2());
                    qpuSelectionResult.setMaxGateTime(qpu.getMaxGateTime());
                    qpuSelectionResult.setQubitCount(qpu.getQubitCount());
                    qpuSelectionResult.setSimulator(qpu.isSimulator());
                    qpuSelectionResult.setUserId(job.getUserId());

                    qpuSelectionResult = qpuSelectionResultRepository.save(qpuSelectionResult);
                    job.getJobResults().add(qpuSelectionResult);
                }
            }
        }

        // store updated result object
        LOG.debug("Results: " + job.getJobResults().size());
        job.setReady(true);
        qpuSelectionJobRepository.save(job);
    }

    /**
     * Compile the given circuit for the given QPU with all supported or a subset of the supported compilers and return the resulting compiled
     * circuits as well as some analysis details
     *
     * @param providerName    the name of the provider of the QPU
     * @param qpuName         the name of the QPU for which the circuit should be compiled
     * @param circuitLanguage the language of the quantum circuit
     * @param circuitCode     the file containing the circuit to compile
     * @param circuitName     user defined name to (partly) distinguish circuits
     * @param compilerNames   an optional list of compiler names to restrict the compilers to use. If not set, all supported compilers are used
     * @param token           the token to access the specified QPU
     * @return the List of compilation results
     */
    private List<CompilationResult> selectCompiler(String providerName, String qpuName, String circuitLanguage,
                                                   File circuitCode, String circuitName, List<String> compilerNames, String token) {
        List<CompilationResult> compilerAnalysisResults = new ArrayList<>();
        LOG.debug("Performing compiler selection for QPU with name '{}' from provider with name '{}'!", qpuName, providerName);
        Qpu qpu = qiskitSdkConnector.getQpuByName(qpuName, providerName, token).orElse(null);

        String initialCircuitAsString = "";
        try {
            initialCircuitAsString = FileUtils.readFileToString(circuitCode, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Unable to read initial circuit as string to store it for later analysis!");
        }

        // retrieve list of compilers that should be used for the comparison
        List<String> compilersToUse;
        if (Objects.nonNull(compilerNames)) {
            LOG.debug("User restricted compiler usage to {} compilers: {}", compilerNames.size(), compilerNames.toString());
            compilersToUse = compilerNames;
        } else {
            compilersToUse = connectorList.stream().flatMap(connector -> connector.supportedSdks().stream()).distinct().collect(Collectors.toList());
            LOG.debug("No restriction for compilers defined. Using all ({}) supported compilers!", compilersToUse.size());
        }

        for (String compilerName : compilersToUse) {
            LOG.debug("Evaluating compiler with name: {}", compilerName);

            // retrieve corresponding connector for the compiler
            Optional<SdkConnector> connectorOptional =
                    connectorList.stream().filter(connector -> connector.supportedSdks().contains(compilerName.toLowerCase())).findFirst();
            if (!connectorOptional.isPresent()) {
                LOG.warn("Unable to find suitable connector for compiler with name: {}", compilerName);
                continue;
            }
            SdkConnector connector = connectorOptional.get();
            LOG.debug("Using connector '{}' to communicate with compiler '{}'", connector.getName(), compilerName);

            // filter compilers that do not support the specified provider
            if (!connector.supportedProviders().contains(providerName.toLowerCase())) {
                LOG.debug("Compiler does not support specified provider. Skipping compilation!");
                continue;
            }

            // translate circuit for the compiler if needed
            File circuitToCompile = circuitCode;
            String circuitToCompileLanguage = circuitLanguage;
            if (!connector.getLanguagesForSdk(compilerName).contains(circuitLanguage.toLowerCase())) {
                LOG.debug("Circuit language '{}' not supported by the compiler. Translating circuit...", circuitLanguage);

                // check if source language is supported by translator
                if (!translatorService.getSupportedLanguages().contains(circuitLanguage.toLowerCase())) {
                    LOG.warn("Unable to transform circuit with unsupported language '{}'!", circuitLanguage);
                    continue;
                }

                // get target language that is supported by the translator and the compiler
                String targetLanguage = connector.getLanguagesForSdk(compilerName.toLowerCase()).stream()
                        .filter(language -> translatorService.getSupportedLanguages().contains(language)).findFirst().orElse(null);
                if (Objects.isNull(targetLanguage)) {
                    LOG.warn("Unable to find target language that is supported by translator and compiler '{}'!", compilerName);
                    continue;
                }

                circuitToCompile = translatorService.tranlateCircuit(circuitCode, circuitLanguage, targetLanguage);
                circuitToCompileLanguage = targetLanguage;

                // skip the compiler if translation into required language failed
                if (Objects.isNull(circuitToCompile)) {
                    LOG.warn("Unable to translate quantum circuit into required language for compiler '{}'. Skipping...", compilerName);
                    continue;
                }
            }

            // compile circuit for the QPU
            LOG.debug("Invoking compilation with circuit language: {}", circuitToCompileLanguage);
            Map<String, ParameterValue> params = new HashMap<>();
            params.put(Constants.TOKEN_PARAMETER, new ParameterValue(DataType.Unknown, token));
            CircuitInformation circuitInformation =
                    connector.getCircuitProperties(circuitToCompile, circuitToCompileLanguage, providerName, qpuName, params);

            if (Objects.isNull(circuitInformation) || Objects.nonNull(circuitInformation.getError())) {
                if (Objects.nonNull(circuitInformation)) {
                    LOG.error("Compilation failed with error: {}", circuitInformation.getError());
                } else {
                    LOG.error("Compilation with compiler '{}' failed!", compilerName);
                }
                continue;
            }

            // check if QPU is simulator or can handle the depth in the current decoherence time
            if (Objects.isNull(qpu) || (qpu.isSimulator() || qpu.getT1() / qpu.getMaxGateTime() >= circuitInformation.getCircuitDepth())) {

                // add resulting compiled circuit to result list
                CompilationResult compilationResult = new CompilationResult();
                compilationResult.setCircuitName(circuitName);
                compilationResult.setTranspiledLanguage(circuitInformation.getTranspiledLanguage());
                compilationResult.setTime(OffsetDateTime.now());
                compilationResult.setInitialCircuit(initialCircuitAsString);
                compilationResult.setTranspiledCircuit(circuitInformation.getTranspiledCircuit());
                compilationResult.setQpu(qpuName);
                compilationResult.setProvider(providerName);
                compilationResult.setCompiler(compilerName);
                compilationResult.setAnalyzedDepth(circuitInformation.getCircuitDepth());
                compilationResult.setAnalyzedWidth(circuitInformation.getCircuitWidth());
                compilationResult.setAnalyzedTotalNumberOfOperations(circuitInformation.getCircuitTotalNumberOfOperations());
                compilationResult.setAnalyzedNumberOfSingleQubitGates(circuitInformation.getCircuitNumberOfSingleQubitGates());
                compilationResult.setAnalyzedNumberOfMeasurementOperations(circuitInformation.getCircuitNumberOfMeasurementOperations());
                compilationResult.setAnalyzedNumberOfMultiQubitGates(circuitInformation.getCircuitNumberOfMultiQubitGates());
                compilationResult.setAnalyzedMultiQubitGateDepth(circuitInformation.getCircuitMultiQubitGateDepth());
                if (Objects.nonNull(qpu)) {
                    compilationResult.setAvgMultiQubitGateError(qpu.getAvgMultiQubitGateError());
                    compilationResult.setAvgMultiQubitGateTime(qpu.getAvgMultiQubitGateTime());
                    compilationResult.setAvgReadoutError(qpu.getAvgReadoutError());
                    compilationResult.setAvgSingleQubitGateError(qpu.getAvgSingleQubitGateError());
                    compilationResult.setAvgSingleQubitGateTime(qpu.getAvgSingleQubitGateTime());
                    compilationResult.setT1(qpu.getT1());
                    compilationResult.setT2(qpu.getT2());
                    compilationResult.setMaxGateTime(qpu.getMaxGateTime());
                    compilationResult.setQubitCount(qpu.getQubitCount());
                    compilationResult.setSimulator(qpu.isSimulator());
                }
                compilationResult.setToken(token);
                compilerAnalysisResults.add(compilationResult);
            }
        }
        return compilerAnalysisResults;
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

            String connectorName = connector.getName();

            if (!prologKnowledgeBaseHandler.doesPrologFileExist(connectorName)) {
                prologFactUpdater.handleSDKConnectorInsertion(connector);
                LOG.debug("Rebuild prolog file for connector {}", connectorName);
            }
        }
    }

    /**
     * rebuild the prolog files for the implementations and qpus, if the app crashs or no prolog files are in temp folder.
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
        LOG.debug("Checking if {} required parameters are available in the input map with {} provided parameters!", requiredParameters.size(),
            providedParameterNames.size());
        return requiredParameters.stream().allMatch(param -> providedParameterNames.contains(param.getName()));
    }

    /**
     * Check if all required parameters are contained in the provided parameters
     *
     * @param provider the provider
     * @return compilers supporting the given provider
     */
    public List<String> getCompilers(String provider) {

        // collect all SDK connectors that support the given provider
        List<SdkConnector> connectors =
            connectorList.stream().filter(connector -> connector.supportedProviders().contains(provider.toLowerCase())).collect(
                Collectors.toList());

        // return a list of all supporting compilers
        return connectors.stream().flatMap(connector -> connector.supportedSdks().stream()).distinct().collect(Collectors.toList());
    }
}
