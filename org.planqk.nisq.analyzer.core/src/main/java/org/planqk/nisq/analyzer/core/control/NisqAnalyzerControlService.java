/*******************************************************************************
 * Copyright (c) 2024 University of Stuttgart
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.CircuitInformationOfImplementation;
import org.planqk.nisq.analyzer.core.connector.OriginalCircuitInformation;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.CompilationJob;
import org.planqk.nisq.analyzer.core.model.CompilationResult;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.OriginalCircuitResult;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.prioritization.restMcdaAndPrediction.PrioritizationService;
import org.planqk.nisq.analyzer.core.qprov.QProvService;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.AnalysisResultRepository;
import org.planqk.nisq.analyzer.core.repository.CompilationJobRepository;
import org.planqk.nisq.analyzer.core.repository.CompilerAnalysisResultRepository;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.OriginalCircuitResultRepository;
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

    final private OriginalCircuitResultRepository originalCircuitResultRepository;

    final private QProvService qProvService;

    final private TranslatorService translatorService;

    final private PrioritizationService prioritizationService;

    final private CompilationJobRepository compilationJobRepository;

    final private AnalysisJobRepository analysisJobRepository;

    final private QpuSelectionJobRepository qpuSelectionJobRepository;

    final private QpuSelectionResultRepository qpuSelectionResultRepository;

    public OriginalCircuitResult analyzeOriginalCircuit(String circuitName, File circuitFile, String circuitLanguage)
        throws UnsatisfiedLinkError {

        // analysis of original circuit with an SDK connector that supports the language
        List<SdkConnector> connectorMatchingList = connectorList.stream()
            .filter(sdkConnector -> sdkConnector.getSupportedLanguages().contains(circuitLanguage.toLowerCase()))
            .collect(Collectors.toList());

        Optional<SdkConnector> connectorOptional =
            connectorMatchingList.stream().filter(sdk -> sdk.getName().equalsIgnoreCase("qiskit")).findFirst();
        if (!connectorOptional.isPresent()) {
            connectorOptional = connectorList.stream()
                .filter(sdkConnector -> sdkConnector.getSupportedLanguages().contains(circuitLanguage.toLowerCase()))
                .findFirst();
        }

        if (connectorOptional.isPresent()) {
            SdkConnector connector = connectorOptional.get();
            OriginalCircuitInformation originalCircuitInformation =
                connector.getOriginalCircuitProperties(circuitFile, circuitLanguage);

            OriginalCircuitResult originalCircuitResult =
                new OriginalCircuitResult(circuitName, originalCircuitInformation.getCircuitWidth(),
                    originalCircuitInformation.getCircuitDepth(),
                    originalCircuitInformation.getCircuitMultiQubitGateDepth(),
                    originalCircuitInformation.getCircuitNumberOfSingleQubitGates(),
                    originalCircuitInformation.getCircuitNumberOfMultiQubitGates(),
                    originalCircuitInformation.getCircuitTotalNumberOfOperations(),
                    originalCircuitInformation.getCircuitNumberOfMeasurementOperations(), null, null);

            return originalCircuitResultRepository.save(originalCircuitResult);
        }

        return null;
    }

    /**
     * Execute the given quantum algorithm implementation with the given input parameters and return the corresponding
     * output of the execution.
     *
     * @param result          the analysis result that shall be executed
     * @param inputParameters the input parameters for the execution as key/value pairs
     * @return the ExecutionResult to track the current status and store the result
     * @throws RuntimeException is thrown in case the execution of the algorithm implementation fails
     */
    /*public ExecutionResult executeQuantumAlgorithmImplementation(AnalysisResult result,
                                                                 Map<String, ParameterValue> inputParameters,
                                                                 String refreshToken) throws RuntimeException {
        final Implementation implementation = result.getImplementation();
        LOG.debug("Executing quantum algorithm implementation with Id: {} and name: {}", implementation.getId(),
            implementation.getName());

        // get suited Sdk connector plugin
        SdkConnector selectedSdkConnector =
            connectorList.stream().filter(executor -> executor.getName().equals(result.getCompiler())).findFirst()
                .orElse(null);
        if (Objects.isNull(selectedSdkConnector)) {
            LOG.error("Unable to find connector plugin with name {}.", result.getCompiler());
            throw new RuntimeException("Unable to find connector plugin with name " + result.getCompiler());
        }

        // Retrieve the QPU from Qiskit-Service
        Optional<Qpu> qpu = qProvService.getQpuByName(result.getQpu(), result.getProvider());
        if (!qpu.isPresent()) {
            LOG.error("Unable to find qpu with name {}.", result.getQpu());
            throw new RuntimeException("Unable to find qpu with name " + result.getQpu());
        }

        // create a object to store the execution results
        ExecutionResult executionResult = executionResultRepository.save(
            new ExecutionResult(ExecutionResultStatus.INITIALIZED, "Passing execution to executor plugin.", result,
                null, null, null, 0, 0, implementation));

        // execute implementation
        new Thread(
            () -> selectedSdkConnector.executeQuantumAlgorithmImplementation(implementation, qpu.get(), inputParameters,
                executionResult, executionResultRepository, refreshToken)).start();

        return executionResult;
    }

    /**
     * Execute the compiled circuit from the given compilation result
     *
     * @param result          the compilation result to execute the circuit for
     * @param inputParameters the input parameters for the execution
     * @return the ExecutionResult to track the current status and store the result
     */
    public ExecutionResult executeCompiledQuantumCircuit(CompilationResult result,
                                                         Map<String, ParameterValue> inputParameters) {

        // get suited Sdk connector plugin
        SdkConnector selectedSdkConnector =
            connectorList.stream().filter(executor -> executor.supportedSdks().contains(result.getCompiler()))
                .findFirst().orElse(null);
        if (Objects.isNull(selectedSdkConnector)) {
            LOG.error("Unable to find connector plugin with name {}.", result.getCompiler());
            throw new RuntimeException("Unable to find connector plugin with name " + result.getCompiler());
        }

        // create a object to store the execution results
        ExecutionResult executionResult = executionResultRepository.save(
            new ExecutionResult(ExecutionResultStatus.INITIALIZED, "Passing execution to executor plugin.", null,
                result, null, null, 0, 0, null));

        // execute implementation
        new Thread(() -> selectedSdkConnector.executeTranspiledQuantumCircuit(result.getTranspiledCircuit(),
            result.getTranspiledLanguage(), result.getProvider(), result.getQpu(), inputParameters, executionResult,
            executionResultRepository, null)).start();

        return executionResult;
    }

    /**
     * Execute the compiled circuit from the given qpu-selection-result
     *
     * @param result          the compilation result to execute the circuit for
     * @param inputParameters the input parameters for the execution
     * @return the ExecutionResult to track the current status and store the result
     */
    public ExecutionResult executeCompiledQpuSelectionCircuit(QpuSelectionResult result,
                                                              Map<String, ParameterValue> inputParameters) {

        // get suited Sdk connector plugin
        SdkConnector selectedSdkConnector =
            connectorList.stream().filter(executor -> executor.supportedSdks().contains(result.getCompiler()))
                .findFirst().orElse(null);
        if (Objects.isNull(selectedSdkConnector)) {
            LOG.error("Unable to find connector plugin with name {}.", result.getCompiler());
            throw new RuntimeException("Unable to find connector plugin with name " + result.getCompiler());
        }

        // create a object to store the execution results
        ExecutionResult executionResult = executionResultRepository.save(
            new ExecutionResult(ExecutionResultStatus.INITIALIZED, "Passing execution to executor plugin.", null, null,
                result, null, 0, 0, null));

        // execute implementation
        new Thread(() -> selectedSdkConnector.executeTranspiledQuantumCircuit(result.getTranspiledCircuit(),
            result.getTranspiledLanguage(), result.getProvider(), result.getQpu(), inputParameters, executionResult,
            executionResultRepository, qpuSelectionResultRepository)).start();

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
     */

    public void performSelection(AnalysisJob job, UUID algorithm, Map<String, String> inputParameters,
                                 String refreshToken, List<String> allowedProviders, List<String> compilers) {
        LOG.debug("Performing quantum resource recommendation for algorithm with Id: {}", algorithm);

        String token = inputParameters.get("token");
        // check all implementation if they can handle the given set of input parameters
        List<Implementation> implementations = implementationRepository.findByImplementedAlgorithm(algorithm);

        LOG.debug("Found {} implementations for the algorithm.", implementations.size());

        List<Implementation> executableImplementations = implementations.stream()
            .filter(implementation -> parametersAvailable(getRequiredParameters(implementation), inputParameters))
            .collect(Collectors.toList());

        List<AnalysisResult> analysisResults = new ArrayList<>();

        for (Implementation implementation : executableImplementations) {

            // Try to infer the type of the parameters for the given implementation
            Map<String, ParameterValue> execInputParameters =
                ParameterValue.inferTypedParameterValue(implementation.getInputParameters(), inputParameters);

            if (!execInputParameters.containsKey("token")) {
                execInputParameters.put(Constants.TOKEN_PARAMETER, new ParameterValue(DataType.Unknown, token));
            }

            // get suited Sdk connector plugin
            SdkConnector selectedSdkConnector = connectorList.stream()
                .filter(executor -> executor.supportedSdks().contains(implementation.getSdk().getName().toLowerCase()))
                .findFirst().orElse(null);
            if (Objects.isNull(selectedSdkConnector)) {
                LOG.error("Unable to find connector plugin with name {}.", implementation.getSdk().getName());
                throw new RuntimeException(
                    "Unable to find connector plugin with name " + implementation.getSdk().getName());
            }

            // generate circuit of implementation based on input parameters and analyze its properties
            CircuitInformationOfImplementation circuitInformationOfImplementation =
                selectedSdkConnector.getCircuitOfImplementation(implementation, execInputParameters, refreshToken);

            // if something unexpected happened
            if (Objects.isNull(circuitInformationOfImplementation)) {
                LOG.error("Circuit generation and analysis by SDK unexpectedly failed.");
                continue;
            }

            inputParameters.remove("token");

            OriginalCircuitResult originalCircuitResult = new OriginalCircuitResult(implementation.getName(),
                circuitInformationOfImplementation.getCircuitWidth(),
                circuitInformationOfImplementation.getCircuitDepth(),
                circuitInformationOfImplementation.getCircuitMultiQubitGateDepth(),
                circuitInformationOfImplementation.getCircuitNumberOfSingleQubitGates(),
                circuitInformationOfImplementation.getCircuitNumberOfMultiQubitGates(),
                circuitInformationOfImplementation.getCircuitTotalNumberOfOperations(),
                circuitInformationOfImplementation.getCircuitNumberOfMeasurementOperations(),
                circuitInformationOfImplementation.getCorrelationId(),
                circuitInformationOfImplementation.getGeneratedCircuit());

            originalCircuitResultRepository.save(originalCircuitResult);

            QpuSelectionJob qpuSelectionJob = new QpuSelectionJob();
            qpuSelectionJob.setTime(OffsetDateTime.now());

            if (implementation.getName() == null) {
                qpuSelectionJob.setCircuitName("temp");
            } else {
                qpuSelectionJob.setCircuitName(implementation.getName());
            }

            qpuSelectionJob = qpuSelectionJobRepository.save(qpuSelectionJob);

            AnalysisResult analysisResult =
                new AnalysisResult(algorithm, implementation, inputParameters, originalCircuitResult.getId(),
                    qpuSelectionJob.getId());
            analysisResult = analysisResultRepository.save(analysisResult);
            analysisResults.add(analysisResult);

            QpuSelectionResult simulatorQpuSelectionResult =
                createAllCircuitQpuCompilerCombinations(qpuSelectionJob, originalCircuitResult, compilers,
                    implementation.getName(), allowedProviders);

            LOG.debug("Generated and analyzed circuit for implementation {}.", implementation.getName());
        }

        job.setJobResults(analysisResults);
        job.setReady(true);
        analysisJobRepository.save(job);

        //TODO: Pre-Selection für alle Impls
        //TODO ggf. Übersetzung
        //TODO Compilation
        //TODO Executable?
        //TODO Prioritization
        //TODO Execution
    }

    public void performSelection(AnalysisJob job, UUID algorithm, Map<String, String> inputParameters,
                                 List<String> allowedProviders, List<String> compilers) throws UnsatisfiedLinkError {
        performSelection(job, algorithm, inputParameters, "", allowedProviders, compilers);
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
        implementationRepository.findByImplementedAlgorithm(algorithm)
            .forEach(impl -> requiredParameters.addAll(getRequiredParameters(impl)));

        return requiredParameters;
    }

    /**
     * Compile the given circuit for the given QPU with all supported or a subset of the supported compilers and return
     * the resulting compiled circuits as well as some analysis details
     *
     * @param job             the compilation job object for the long-running task
     * @param providerName    the name of the provider of the QPU
     * @param qpuName         the name of the QPU for which the circuit should be compiled
     * @param circuitLanguage the language of the quantum circuit
     * @param circuitCode     the file containing the circuit to compile
     * @param circuitName     user defined name to (partly) distinguish circuits
     * @param compilerNames   an optional list of compiler names to restrict the compilers to use. If not set, all
     *                        supported compilers are used
     * @param tokens          the tokens to access the specified QPU
     */
    public void performCompilerSelection(CompilationJob job, String providerName, String qpuName,
                                         String circuitLanguage, File circuitCode, String circuitName,
                                         List<String> compilerNames, Map<String, String> tokens) {

        // analyze compilers and retrieve suitable compilation results
        List<CompilationResult> compilerAnalysisResults =
            selectCompiler(providerName, qpuName, circuitLanguage, circuitCode, circuitName, compilerNames, tokens);

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
     * @param job              the QPU selection job for the long-running task
     * @param allowedProviders an optional list with providers to include into the selection. If not specified all
     *                         providers are taken into account.
     * @param circuitLanguage  the language of the circuit for which the QPU selection should be performed
     * @param circuitCode      the file containing the circuit
     * @param tokens           a map with access tokens for the different quantum hardware providers
     * @param circuitName      user defined name to (partly) distinguish circuits
     */
    @Transactional
    public void performQpuSelectionForCircuit(QpuSelectionJob job, List<String> allowedProviders,
                                              String circuitLanguage, File circuitCode,
                                              Map<String, Map<String, String>> tokens, String circuitName,
                                              List<String> compilers, boolean preciseResultsPreference,
                                              boolean shortWaitingTimesPreference, Float queueImportanceRatio,
                                              int maxNumberOfCompiledCircuits, String predictionAlgorithm,
                                              String metaOptimizer) {

        // make name of providers case-insensitive
        TreeMap<String, Map<String, String>> caseInsensitiveTokens = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveTokens.putAll(tokens);

        // analyze original circuit for pre-selection
        OriginalCircuitResult originalCircuitResult = analyzeOriginalCircuit(circuitName, circuitCode, circuitLanguage);

        // generate all possible combinations
        QpuSelectionResult simulatorQpuSelectionResult =
            createAllCircuitQpuCompilerCombinations(job, originalCircuitResult, compilers, circuitName,
                allowedProviders);

        // user prefers only short waiting times
        if (shortWaitingTimesPreference && !preciseResultsPreference) {
            job.getJobResults().sort(Comparator.comparing(QpuSelectionResult::getQueueSize));
            // trim list of possible combinations
            if (maxNumberOfCompiledCircuits > 0 && job.getJobResults().size() > maxNumberOfCompiledCircuits) {
                job.getJobResults().subList(maxNumberOfCompiledCircuits, job.getJobResults().size()).clear();

                // add one compilation candidate for the simulator to calculate the histogram intersection after
                // execution
                AtomicBoolean simulatorAlreadyContained = new AtomicBoolean(false);
                job.getJobResults().forEach(qpuSelectionResult -> {
                    if (qpuSelectionResult.getQpu().contains("simulator")) {
                        simulatorAlreadyContained.set(true);
                    }
                });
                if (!simulatorAlreadyContained.get()) {
                    simulatorQpuSelectionResult = qpuSelectionResultRepository.save(simulatorQpuSelectionResult);
                    job.getJobResults().add(simulatorQpuSelectionResult);
                }
            }
        } else if (preciseResultsPreference) {
            // look up if there is prior data for pre-selection based on prediction of precise execution results
            // available
            boolean priorDataAvailable = false;
            List<ExecutionResult> executionResultList = executionResultRepository.findAll();
            for (ExecutionResult executionResult : executionResultList) {
                // ignore simulator results that are equal to 1 and non-calculated values
                if (executionResult.getHistogramIntersectionValue() > 0 &&
                    executionResult.getHistogramIntersectionValue() < 1) {
                    priorDataAvailable = true;
                    break;
                }
            }
            if (priorDataAvailable) {
                List<String> qpuSelectionResultIdList =
                    prioritizationService.executePredictionForCompilerAnQpuPreSelection(originalCircuitResult, job,
                        queueImportanceRatio, predictionAlgorithm, metaOptimizer, shortWaitingTimesPreference);

                if (qpuSelectionResultIdList != null) {
                    // trim list of possible combinations
                    if (maxNumberOfCompiledCircuits > 0 &&
                        qpuSelectionResultIdList.size() > maxNumberOfCompiledCircuits) {
                        qpuSelectionResultIdList.subList(maxNumberOfCompiledCircuits, qpuSelectionResultIdList.size())
                            .clear();
                        List<QpuSelectionResult> listOfSelectedQpuSelectionResults = new ArrayList<>();
                        qpuSelectionResultIdList.forEach(id -> {
                            job.getJobResults().forEach(qpuSelectionResult -> {
                                if (qpuSelectionResult.getId().equals(UUID.fromString(id))) {
                                    listOfSelectedQpuSelectionResults.add(qpuSelectionResult);
                                }
                            });
                        });
                        job.setJobResults(listOfSelectedQpuSelectionResults);

                        // add one compilation candidate for the simulator to calculate the histogram intersection after
                        // execution
                        AtomicBoolean simulatorAlreadyContained = new AtomicBoolean(false);
                        job.getJobResults().forEach(qpuSelectionResult -> {
                            if (qpuSelectionResult.getQpu().contains("simulator")) {
                                simulatorAlreadyContained.set(true);
                            }
                        });
                        if (!simulatorAlreadyContained.get()) {
                            simulatorQpuSelectionResult =
                                qpuSelectionResultRepository.save(simulatorQpuSelectionResult);
                            job.getJobResults().add(simulatorQpuSelectionResult);
                        }
                    }
                }
            }
        }

        // delete compilation candidates that will not be considered
        qpuSelectionResultRepository.findAllByQpuSelectionJobId(job.getId()).forEach(qpuSelectionResult -> {
            AtomicBoolean resultIsContained = new AtomicBoolean(false);
            job.getJobResults().forEach(qpuSelectionResult1 -> {
                if (qpuSelectionResult.getId().equals(qpuSelectionResult1.getId())) {
                    resultIsContained.set(true);
                }
            });
            if (!resultIsContained.get()) {
                qpuSelectionResultRepository.delete(qpuSelectionResult);
            }
        });

        // overwrite old, non-updated qpuSelectionResults in the job, after prediction
        List<QpuSelectionResult> remainingQpuSelectionResultList = new ArrayList<>();
        job.getJobResults().forEach(qpuSelectionResultOld -> {
            Optional<QpuSelectionResult> qpuSelectionResultOptional =
                qpuSelectionResultRepository.findById(qpuSelectionResultOld.getId());
            qpuSelectionResultOptional.ifPresent(remainingQpuSelectionResultList::add);
        });
        job.setJobResults(remainingQpuSelectionResultList);

        job.getJobResults().forEach(qpuSelectionResult -> {
            Map<String, String> tokensOfProvider = caseInsensitiveTokens.get(qpuSelectionResult.getProvider());
            if (Objects.isNull(tokensOfProvider)) {
                LOG.debug("No suited access token for this provider available. Skipping!");
            }

            List<String> compilerNames = new ArrayList<>();
            compilerNames.add(qpuSelectionResult.getCompiler());
            // perform compiler selection for the given QPU and circuit
            List<CompilationResult> compilationResults =
                selectCompiler(qpuSelectionResult.getProvider(), qpuSelectionResult.getQpu(), circuitLanguage,
                    circuitCode, qpuSelectionResult.getCircuitName(), compilerNames, tokensOfProvider);
            LOG.debug("Retrieved {} compilation results!", compilationResults.size());

            if (compilationResults.size() > 0) {
                CompilationResult result = compilationResults.get(0);
                // add compilation result to the database
                qpuSelectionResult.setTranspiledCircuit(result.getTranspiledCircuit());
                qpuSelectionResult.setTranspiledLanguage(result.getTranspiledLanguage());
                qpuSelectionResult.setAnalyzedDepth(result.getAnalyzedDepth());
                qpuSelectionResult.setAnalyzedWidth(result.getAnalyzedWidth());
                qpuSelectionResult.setAnalyzedTotalNumberOfOperations(result.getAnalyzedTotalNumberOfOperations());
                qpuSelectionResult.setAnalyzedNumberOfSingleQubitGates(result.getAnalyzedNumberOfSingleQubitGates());
                qpuSelectionResult.setAnalyzedNumberOfMeasurementOperations(
                    result.getAnalyzedNumberOfMeasurementOperations());
                qpuSelectionResult.setAnalyzedNumberOfMultiQubitGates(result.getAnalyzedNumberOfMultiQubitGates());
                qpuSelectionResult.setAnalyzedMultiQubitGateDepth(result.getAnalyzedMultiQubitGateDepth());

                qpuSelectionResultRepository.save(qpuSelectionResult);
            }
        });

        //delete qpuSelectionResults that are not executable because they were not compilable as too many qubits are
        // required
        qpuSelectionResultRepository.findAllByQpuSelectionJobId(job.getId()).forEach(qpuSelectionResult -> {
            if (qpuSelectionResult.getAnalyzedWidth() == 0 && qpuSelectionResult.getAnalyzedDepth() == 0) {
                qpuSelectionResultRepository.delete(qpuSelectionResult);
            }
        });
        List<QpuSelectionResult> allOverRemainingQpuSelectionResultList = new ArrayList<>();
        job.getJobResults().forEach(qpuSelectionResultOld -> {
            Optional<QpuSelectionResult> qpuSelectionResultOptional =
                qpuSelectionResultRepository.findById(qpuSelectionResultOld.getId());
            qpuSelectionResultOptional.ifPresent(allOverRemainingQpuSelectionResultList::add);
        });
        job.setJobResults(allOverRemainingQpuSelectionResultList);

        // store updated result object
        LOG.debug("Results: " + job.getJobResults().size());
        job.setReady(true);
        qpuSelectionJobRepository.save(job);
    }

    /**
     * Compile the given circuit for the given QPU with all supported or a subset of the supported compilers and return
     * the resulting compiled circuits as well as some analysis details
     *
     * @param providerName    the name of the provider of the QPU
     * @param qpuName         the name of the QPU for which the circuit should be compiled
     * @param circuitLanguage the language of the quantum circuit
     * @param circuitCode     the file containing the circuit to compile
     * @param circuitName     user defined name to (partly) distinguish circuits
     * @param compilerNames   an optional list of compiler names to restrict the compilers to use. If not set, all
     *                        supported compilers are used
     * @param tokens          the tokens to access the specified QPU
     * @return the List of compilation results
     */
    private List<CompilationResult> selectCompiler(String providerName, String qpuName, String circuitLanguage,
                                                   File circuitCode, String circuitName, List<String> compilerNames,
                                                   Map<String, String> tokens) {
        List<CompilationResult> compilerAnalysisResults = new ArrayList<>();
        LOG.debug("Performing compiler selection for QPU with name '{}' from provider with name '{}'!", qpuName,
            providerName);
        Qpu qpu = qProvService.getQpuByName(qpuName, providerName).orElse(null);
        if (Objects.isNull(qpu) & qpuName.contains("simulator") & providerName.contains("ibmq")) {
            qpu = new Qpu();
            qpu.setProvider("ibmq");
            qpu.setSimulator(true);
            qpu.setQueueSize(0);
            qpu.setQubitCount(32);
            qpu.setT1(0);
            qpu.setT2(0);
            qpu.setAvgMultiQubitGateError(0);
            qpu.setAvgReadoutError(0);
            qpu.setAvgSingleQubitGateError(0);
            qpu.setMaxGateTime(0);
            qpu.setAvgSingleQubitGateTime(0);
            qpu.setAvgMultiQubitGateTime(0);
            qpu.setName("aer_simulator");
        }

        String initialCircuitAsString = "";
        try {
            initialCircuitAsString = FileUtils.readFileToString(circuitCode, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Unable to read initial circuit as string to store it for later analysis!");
        }

        // retrieve list of compilers that should be used for the comparison
        List<String> compilersToUse;
        if (Objects.nonNull(compilerNames)) {
            LOG.debug("User restricted compiler usage to {} compilers: {}", compilerNames.size(),
                compilerNames.toString());
            compilersToUse = compilerNames;
        } else {
            compilersToUse = connectorList.stream().flatMap(connector -> connector.supportedSdks().stream()).distinct()
                .collect(Collectors.toList());
            LOG.debug("No restriction for compilers defined. Using all ({}) supported compilers!",
                compilersToUse.size());
        }

        for (String compilerName : compilersToUse) {
            LOG.debug("Evaluating compiler with name: {}", compilerName);

            // retrieve corresponding connector for the compiler
            Optional<SdkConnector> connectorOptional = connectorList.stream()
                .filter(connector -> connector.supportedSdks().contains(compilerName.toLowerCase())).findFirst();
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
                LOG.debug("Circuit language '{}' not supported by the compiler. Translating circuit...",
                    circuitLanguage);

                // check if source language is supported by translator
                if (!translatorService.getSupportedLanguages().contains(circuitLanguage.toLowerCase())) {
                    LOG.warn("Unable to transform circuit with unsupported language '{}'!", circuitLanguage);
                    continue;
                }

                // get target language that is supported by the translator and the compiler
                String targetLanguage = connector.getLanguagesForSdk(compilerName.toLowerCase()).stream()
                    .filter(language -> translatorService.getSupportedLanguages().contains(language)).findFirst()
                    .orElse(null);
                if (Objects.isNull(targetLanguage)) {
                    LOG.warn("Unable to find target language that is supported by translator and compiler '{}'!",
                        compilerName);
                    continue;
                }

                circuitToCompile = translatorService.tranlateCircuit(circuitCode, circuitLanguage, targetLanguage);
                circuitToCompileLanguage = targetLanguage;

                // skip the compiler if translation into required language failed
                if (Objects.isNull(circuitToCompile)) {
                    LOG.warn(
                        "Unable to translate quantum circuit into required language for compiler '{}'. Skipping...",
                        compilerName);
                    continue;
                }
            }

            // compile circuit for the QPU
            LOG.debug("Invoking compilation with circuit language: {}", circuitToCompileLanguage);
            Map<String, ParameterValue> params = new HashMap<>();
            if (providerName.equalsIgnoreCase("ibmq")) {
                params.put(Constants.TOKEN_PARAMETER, new ParameterValue(DataType.Unknown, tokens.get("ibmq")));
            } else if (providerName.equalsIgnoreCase("ionq")) {
                params.put(Constants.TOKEN_PARAMETER, new ParameterValue(DataType.Unknown, tokens.get("ionq")));
            } else if (providerName.equalsIgnoreCase("aws")) {
                params.put(Constants.AWS_ACCESS_TOKEN_PARAMETER,
                    new ParameterValue(DataType.Unknown, tokens.get("awsAccessKey")));
                params.put(Constants.AWS_ACCESS_SECRET_PARAMETER,
                    new ParameterValue(DataType.Unknown, tokens.get("awsSecretKey")));
            }
            CircuitInformation circuitInformation =
                connector.getCircuitProperties(circuitToCompile, circuitToCompileLanguage, providerName, qpu.getName(),
                    params);

            if (Objects.isNull(circuitInformation) || Objects.nonNull(circuitInformation.getError())) {
                if (Objects.nonNull(circuitInformation)) {
                    LOG.error("Compilation failed with error: {}", circuitInformation.getError());
                } else {
                    LOG.error("Compilation with compiler '{}' failed!", compilerName);
                }
                continue;
            }

            // check if QPU is simulator or can handle the depth in the current decoherence time
            if (Objects.isNull(qpu) || (qpu.isSimulator() ||
                (qpu.getT1() / qpu.getMaxGateTime() >= circuitInformation.getCircuitDepth() &&
                    qpu.getQubitCount() >= circuitInformation.getCircuitWidth()))) {

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
                compilationResult.setAnalyzedTotalNumberOfOperations(
                    circuitInformation.getCircuitTotalNumberOfOperations());
                compilationResult.setAnalyzedNumberOfSingleQubitGates(
                    circuitInformation.getCircuitNumberOfSingleQubitGates());
                compilationResult.setAnalyzedNumberOfMeasurementOperations(
                    circuitInformation.getCircuitNumberOfMeasurementOperations());
                compilationResult.setAnalyzedNumberOfMultiQubitGates(
                    circuitInformation.getCircuitNumberOfMultiQubitGates());
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
                compilerAnalysisResults.add(compilationResult);
            }
        }
        return compilerAnalysisResults;
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

        return requiredParameters;
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
        LOG.debug("Checking if {} required parameters are available in the input map with {} provided parameters!",
            requiredParameters.size(), providedParameterNames.size());
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
            connectorList.stream().filter(connector -> connector.supportedProviders().contains(provider.toLowerCase()))
                .collect(Collectors.toList());

        // return a list of all supporting compilers
        return connectors.stream().flatMap(connector -> connector.supportedSdks().stream()).distinct()
            .collect(Collectors.toList());
    }

    private QpuSelectionResult createAllCircuitQpuCompilerCombinations(QpuSelectionJob job,
                                                                       OriginalCircuitResult originalCircuitResult,
                                                                       List<String> compilers, String circuitName,
                                                                       List<String> allowedProviders) {

        if (circuitName == null) {
            circuitName = "temp";
        }

        QpuSelectionResult simulatorQpuSelectionResult = new QpuSelectionResult();
        simulatorQpuSelectionResult.setSimulator(true);
        simulatorQpuSelectionResult.setT1(0);
        simulatorQpuSelectionResult.setT2(0);
        simulatorQpuSelectionResult.setMaxGateTime(0);
        simulatorQpuSelectionResult.setQubitCount(32);
        simulatorQpuSelectionResult.setQpu("aer_simulator");
        simulatorQpuSelectionResult.setCompiler("qiskit");
        simulatorQpuSelectionResult.setTime(OffsetDateTime.now());
        simulatorQpuSelectionResult.setOriginalCircuitResultId(originalCircuitResult.getId());
        simulatorQpuSelectionResult.setQueueSize(0);
        simulatorQpuSelectionResult.setProvider("ibmq");
        simulatorQpuSelectionResult.setCircuitName(circuitName);
        simulatorQpuSelectionResult.setQpuSelectionJobId(job.getId());
        simulatorQpuSelectionResult.setAvgMultiQubitGateError(0);
        simulatorQpuSelectionResult.setAvgMultiQubitGateTime(0);
        simulatorQpuSelectionResult.setAvgReadoutError(0);
        simulatorQpuSelectionResult.setAvgSingleQubitGateError(0);
        simulatorQpuSelectionResult.setAvgSingleQubitGateTime(0);
        simulatorQpuSelectionResult.setUserId(job.getUserId());

        // retrieve list of compilers that should be used for the comparison
        List<String> compilersToUse;
        if (Objects.nonNull(compilers)) {
            LOG.debug("User restricted compiler usage to {} compilers: {}", compilers.size(), compilers.toString());
            compilersToUse = compilers;
        } else {
            compilersToUse = connectorList.stream().flatMap(connector -> connector.supportedSdks().stream()).distinct()
                .collect(Collectors.toList());
            LOG.debug("No restriction for compilers defined. Using all ({}) supported compilers!",
                compilersToUse.size());
        }

        // iterate over all providers listed in QProv for the QPU selection
        for (Provider provider : qProvService.getProviders()) {

            // filter providers that are not contained in the list of allowed providers
            if (Objects.nonNull(allowedProviders) && allowedProviders.stream()
                .noneMatch(allowedProvider -> allowedProvider.equalsIgnoreCase(provider.getName()))) {
                LOG.debug("Provider '{}' is not contained in list of allowed providers. Skipping!", provider.getName());
                continue;
            }

            LOG.debug("Performing QPU selection for provider with name: {}", provider.getName());

            // get available QPUs
            List<Qpu> qpus = qProvService.getQPUs(provider);
            LOG.debug("Found {} QPUs from provider '{}'!", qpus.size(), provider.getName());

            for (Qpu qpu : qpus) {
                LOG.debug("Checking if QPU '{}' is suitable for the execution of the given circuit...", qpu.getName());

//                if (!simulatorsAllowed && qpu.isSimulator()) {
//                    LOG.debug("Skipping simulator as they are disabled for the selection!");
//                    continue;
//                }

                // only consider standard simulator
                if (provider.getName().equalsIgnoreCase("ibmq") && qpu.isSimulator() &&
                    !Objects.equals(qpu.getName(), "ibmq_qasm_simulator")) {
                    continue;
                }

                // create all possible combinations of QPUs and compilers and retrieve actual QPU data
                for (String compilerName : compilersToUse) {
                    // use only qiskit transpiler when compiling with the simulator
                    if (!(compilerName.equalsIgnoreCase("pytket") &&
                        qpu.getName().equalsIgnoreCase("ibmq_qasm_simulator"))) {
                        QpuSelectionResult qpuSelectionResult = new QpuSelectionResult();
                        qpuSelectionResult.setCircuitName(circuitName);
                        qpuSelectionResult.setTime(OffsetDateTime.now());
                        qpuSelectionResult.setQueueSize(qpu.getQueueSize());
                        qpuSelectionResult.setQpu(qpu.getName());
                        qpuSelectionResult.setProvider(provider.getName());
                        qpuSelectionResult.setCompiler(compilerName);
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
                        qpuSelectionResult.setOriginalCircuitResultId(originalCircuitResult.getId());

                        qpuSelectionResult = qpuSelectionResultRepository.save(qpuSelectionResult);
                        job.getJobResults().add(qpuSelectionResult);

                        if (qpuSelectionResult.getQpu().contains("qasm_simulator")) {
                            simulatorQpuSelectionResult = qpuSelectionResult;
                        }
                    }
                }
            }
        }
        return simulatorQpuSelectionResult;
    }
}
