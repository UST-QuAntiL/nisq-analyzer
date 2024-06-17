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

package org.planqk.nisq.analyzer.core.connector.qiskit;

import static org.planqk.nisq.analyzer.core.web.Utils.getBearerTokenFromRefreshToken;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.CircuitInformationOfImplementation;
import org.planqk.nisq.analyzer.core.connector.ExecutionRequestResult;
import org.planqk.nisq.analyzer.core.connector.OriginalCircuitInformation;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Sdk connector which passes execution and analysis requests to a connected Qiskit service.
 */
@Service
public class QiskitSdkConnector implements SdkConnector {

    final private static Logger LOG = LoggerFactory.getLogger(QiskitSdkConnector.class);

    // API Endpoints
    private final URI generateAPIEndpoint;

    private final URI transpileAPIEndpoint;

    private final URI executeAPIEndpoint;

    private final URI analyzeOriginalAPIEndpoint;

    @Value("${org.planqk.nisq.analyzer.connector.qiskit.pollInterval:10000}")
    private int pollInterval;

    public QiskitSdkConnector(@Value("${org.planqk.nisq.analyzer.connector.qiskit.hostname}") String hostname,
                              @Value("${org.planqk.nisq.analyzer.connector.qiskit.port}") int port,
                              @Value("${org.planqk.nisq.analyzer.connector.qiskit.version}") String version) {
        // compile the API endpoints
        generateAPIEndpoint =
            URI.create(String.format("http://%s:%d/qiskit-service/api/%s/generate-circuit", hostname, port, version));
        analyzeOriginalAPIEndpoint = URI.create(
            String.format("http://%s:%d/qiskit-service/api/%s/analyze-original-circuit", hostname, port, version));
        transpileAPIEndpoint =
            URI.create(String.format("http://%s:%d/qiskit-service/api/%s/transpile", hostname, port, version));
        executeAPIEndpoint =
            URI.create(String.format("http://%s:%d/qiskit-service/api/%s/execute", hostname, port, version));
    }

    public CircuitInformationOfImplementation getCircuitOfImplementation(Implementation implementation,
                                                                         Map<String, ParameterValue> parameters,
                                                                         String refreshToken) {
        LOG.debug(
            "Generating and analyzing quantum circuit of quantum algorithm implementation with Qiskit Sdk connector " +
                "plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        QiskitRequest request =
            new QiskitRequest(implementation.getFileLocation(), implementation.getLanguage(), parameters, bearerToken);

        RestTemplate restTemplate = new RestTemplate();
        try {
            // request to generate circuit
            URI circuitLocation = restTemplate.postForLocation(generateAPIEndpoint, request);

            ExecutionResultStatus generationComplete = ExecutionResultStatus.RUNNING;

            // poll the Qiskit service frequently
            while (generationComplete != ExecutionResultStatus.FAILED) {
                try {
                    ResponseEntity<CircuitInformationOfImplementation> response =
                        restTemplate.exchange(circuitLocation, HttpMethod.GET, null,
                            CircuitInformationOfImplementation.class);

                    CircuitInformationOfImplementation result = null;

                    // Check if the Qiskit service was successful
                    if (response.getStatusCode().is2xxSuccessful()) {
                        LOG.debug("Generating circuit using Qiskit Service.");
                        result = response.getBody();
                        result.setCircuitLanguage(Constants.OPENQASM);

                        // Check if generation is completed
                        if (result.isComplete()) {
                            generationComplete = ExecutionResultStatus.FINISHED;
                            return result;
                        }
                    } else if (response.getStatusCode().is4xxClientError()) {
                        LOG.error(
                            String.format("Qiskit Service rejected request (HTTP %d)", response.getStatusCodeValue()));
                        generationComplete = ExecutionResultStatus.FAILED;
                    } else if (response.getStatusCode().is5xxServerError()) {
                        LOG.error(
                            String.format("Internal Qiskit Service error (HTTP %d)", response.getStatusCodeValue()));
                        generationComplete = ExecutionResultStatus.FAILED;
                    }

                    // Wait for next poll
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e) {
                        // pass
                    }
                } catch (RestClientException e) {
                    LOG.error("Polling generation result from Qiskit Service failed.");
                    generationComplete = ExecutionResultStatus.FAILED;
                }
            }
        } catch (RestClientException e) {
            LOG.error("Circuit generation with Qiskit Service failed.");
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void executeQuantumAlgorithmImplementation(Implementation implementation, Qpu qpu,
                                                      Map<String, ParameterValue> parameters,
                                                      ExecutionResult executionResult,
                                                      ExecutionResultRepository resultRepository, String refreshToken) {
        LOG.debug("Executing quantum algorithm implementation with Qiskit Sdk connector plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        QiskitRequest request =
            new QiskitRequest(implementation.getFileLocation(), implementation.getLanguage(), qpu.getName(),
                qpu.getProvider(), parameters, bearerToken);
        executeQuantumCircuit(request, executionResult, resultRepository, null);
    }

    @Override
    public void executeTranspiledQuantumCircuit(String transpiledCircuit, String transpiledLanguage,
                                                String providerName, String qpuName,
                                                Map<String, ParameterValue> parameters, ExecutionResult executionResult,
                                                ExecutionResultRepository resultRepository,
                                                QpuSelectionResultRepository qpuSelectionResultRepository) {
        LOG.debug("Executing circuit passed as file with provider '{}' and qpu '{}'.", providerName, qpuName);
        QiskitRequest request = new QiskitRequest(transpiledCircuit, qpuName, providerName, parameters);
        executeQuantumCircuit(request, executionResult, resultRepository, qpuSelectionResultRepository);
    }

    private void executeQuantumCircuit(QiskitRequest request, ExecutionResult executionResult,
                                       ExecutionResultRepository resultRepository,
                                       QpuSelectionResultRepository qpuSelectionResultRepository) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // make the execution request
            URI resultLocation = restTemplate.postForLocation(executeAPIEndpoint, request);

            // change the result status
            executionResult.setStatus(ExecutionResultStatus.RUNNING);
            executionResult.setStatusCode("Pending for execution on Qiskit Service ...");
            resultRepository.save(executionResult);

            // poll the Qiskit service frequently
            while (executionResult.getStatus() != ExecutionResultStatus.FINISHED &&
                executionResult.getStatus() != ExecutionResultStatus.FAILED) {
                try {
                    ExecutionRequestResult result =
                        restTemplate.getForObject(resultLocation, ExecutionRequestResult.class);

                    // Check if execution is completed
                    if (result.isComplete()) {
                        executionResult.setStatus(ExecutionResultStatus.FINISHED);
                        executionResult.setStatusCode("Execution successfully completed.");
                        executionResult.setResult(result.getResult().toString());
                        executionResult.setShots(result.getShots());

                        // histogram intersection
                        //FIXME currently only for qpu-selection
                        if (Objects.nonNull(qpuSelectionResultRepository)) {
                            Optional<QpuSelectionResult> qpuSelectionResult =
                                qpuSelectionResultRepository.findById(executionResult.getQpuSelectionResult().getId());
                            if (qpuSelectionResult.isPresent()) {
                                // get stored token for the execution
                                QpuSelectionResult qResult = qpuSelectionResult.get();

                                // as QiskitSdk Connector is invoked, the provider is IBM Q, thus, the
                                // ibmq simulator is the one we need
                                // for histogram intersection
                                String simulator = "simulator";

                                // check if current execution result is already of a simulator otherwise get all
                                // qpu-selection-results of same job
                                if (!qResult.getQpu().contains(simulator)) {
                                    List<QpuSelectionResult> jobResults =
                                        qpuSelectionResultRepository.findAllByQpuSelectionJobId(
                                            qResult.getQpuSelectionJobId());
                                    // get qpuSelectionResult of simulator if available
                                    QpuSelectionResult simulatorQpuSelectionResult =
                                        jobResults.stream().filter(jobResult -> jobResult.getQpu().contains(simulator))
                                            .findFirst().orElse(null);
                                    if (Objects.nonNull(simulatorQpuSelectionResult)) {
                                        //check if qpu-selection result of simulator was already executed otherwise
                                        // wait max 1 minute
                                        int iterator = 60;
                                        while (iterator > 0) {
                                            try {
                                                Thread.sleep(1000);
                                                ExecutionResult simulatorExecutionResult =
                                                    resultRepository.findAll().stream().filter(
                                                            exResults -> Objects.nonNull(exResults.getQpuSelectionResult()))
                                                        .filter(exeResult -> exeResult.getQpuSelectionResult().getId()
                                                            .equals(simulatorQpuSelectionResult.getId())).findFirst()
                                                        .orElse(null);

                                                // as soon as execution result of simulator is returned calculate
                                                // histogram intersection
                                                if (Objects.nonNull(simulatorExecutionResult)) {
                                                    // convert stored execution result of simulator to Map
                                                    String simulatorExecutionResultString =
                                                        simulatorExecutionResult.getResult();
                                                    Map<String, Integer> simulatorCountsOfResults = new HashMap<>();
                                                    String rawData =
                                                        simulatorExecutionResultString.replaceAll("[\\{\\}\\s+]", "");
                                                    String[] instances = rawData.split(",");
                                                    for (String instance : instances) {
                                                        String[] resultsData = instance.split("=");
                                                        String measurementResult = resultsData[0].trim();
                                                        int counts = Integer.parseInt(resultsData[1].trim());
                                                        simulatorCountsOfResults.put(measurementResult, counts);
                                                    }

                                                    Map<String, Integer> qpuExecutionResult = result.getResult();
                                                    // histogram intersection calculation
                                                    double intersection = 0;
                                                    for (String qpuKey : qpuExecutionResult.keySet()) {
                                                        if (!simulatorCountsOfResults.containsKey(qpuKey)) {
                                                            simulatorCountsOfResults.put(qpuKey, 0);
                                                        }
                                                    }
                                                    for (String simulatorKey : simulatorCountsOfResults.keySet()) {
                                                        if (!qpuExecutionResult.containsKey(simulatorKey)) {
                                                            qpuExecutionResult.put(simulatorKey, 0);
                                                        }
                                                        intersection = intersection +
                                                            Math.min(simulatorCountsOfResults.get(simulatorKey),
                                                                qpuExecutionResult.get(simulatorKey));
                                                    }
                                                    if (intersection > 0) {
                                                        executionResult.setHistogramIntersectionValue(
                                                            intersection / simulatorExecutionResult.getShots());
                                                    }
                                                    break;
                                                }
                                                iterator--;
                                            } catch (InterruptedException e) {
                                                // pass
                                            }
                                        }
                                    }
                                } else {
                                    executionResult.setHistogramIntersectionValue(1);
                                }
                            }
                        }
                        resultRepository.save(executionResult);
                    }

                    // Wait for next poll
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e) {
                        // pass
                    }
                } catch (RestClientException e) {
                    LOG.error("Polling result from Qiskit Service failed.");
                    executionResult.setStatus(ExecutionResultStatus.FAILED);
                    executionResult.setStatusCode("Polling result from Qiskit Service failed.");
                    resultRepository.save(executionResult);
                }
            }
        } catch (RestClientException e) {
            LOG.error("Connection to Qiskit Service failed.");
            executionResult.setStatus(ExecutionResultStatus.FAILED);
            executionResult.setStatusCode("Connection to Qiskit Service failed.");
            resultRepository.save(executionResult);
        }
    }

    private OriginalCircuitInformation executeOriginalCircuitPropertiesRequest(QiskitRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Analyze the given original circuit using Qiskit service
            ResponseEntity<OriginalCircuitInformation> response =
                restTemplate.postForEntity(analyzeOriginalAPIEndpoint, request, OriginalCircuitInformation.class);

            // Check if the Qiskit service was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.debug("Original circuit analyzing using Qiskit Service.");

                return response.getBody();
            } else if (response.getStatusCode().is4xxClientError()) {
                LOG.error(String.format("Qiskit Service rejected request (HTTP %d)", response.getStatusCodeValue()));
            } else if (response.getStatusCode().is5xxServerError()) {
                LOG.error(String.format("Internal Qiskit Service error (HTTP %d)", response.getStatusCodeValue()));
            }
        } catch (RestClientException e) {
            LOG.error("Connection to Qiskit Service failed.");
        }

        return null;
    }

    @Override
    public CircuitInformation getCircuitProperties(Implementation implementation, String providerName, String qpuName,
                                                   Map<String, ParameterValue> parameters, String refreshToken) {
        LOG.debug("Analysing quantum algorithm implementation with Qiskit Sdk connector plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        QiskitRequest request =
            new QiskitRequest(implementation.getFileLocation(), implementation.getLanguage(), qpuName, providerName,
                parameters, bearerToken);
        return executeCircuitPropertiesRequest(request);
    }

    @Override
    public CircuitInformation getCircuitProperties(File circuit, String language, String providerName, String qpuName,
                                                   Map<String, ParameterValue> parameters) {
        LOG.debug(
            "Retrieving circuit properties for circuit passed as file with provider '{}', qpu '{}', and language '{}'.",
            providerName, qpuName, language);
        try {
            // retrieve content form file and encode base64
            String fileContent = FileUtils.readFileToString(circuit, StandardCharsets.UTF_8);
            String encodedCircuit = Base64.getEncoder().encodeToString(fileContent.getBytes());
            QiskitRequest request = new QiskitRequest(language, encodedCircuit, qpuName, providerName, parameters);
            return executeCircuitPropertiesRequest(request);
        } catch (IOException e) {
            LOG.error("Unable to read file content from circuit file!");
        }
        return null;
    }

    private CircuitInformation executeCircuitPropertiesRequest(QiskitRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Transpile the given algorithm implementation using Qiskit service
            ResponseEntity<CircuitInformation> response =
                restTemplate.postForEntity(transpileAPIEndpoint, request, CircuitInformation.class);

            // Check if the Qiskit service was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.debug("Circuit transpiled using Qiskit Service.");
                CircuitInformation circuitInformation = response.getBody();

                // update language required for selection
                if (Objects.nonNull(circuitInformation)) {
                    circuitInformation.setTranspiledLanguage(Constants.OPENQASM);
                }
                return circuitInformation;
            } else if (response.getStatusCode().is4xxClientError()) {
                LOG.error(String.format("Qiskit Service rejected request (HTTP %d)", response.getStatusCodeValue()));
            } else if (response.getStatusCode().is5xxServerError()) {
                LOG.error(String.format("Internal Qiskit Service error (HTTP %d)", response.getStatusCodeValue()));
            }
        } catch (RestClientException e) {
            LOG.error("Connection to Qiskit Service failed.");
        }

        return null;
    }

    @Override
    public List<String> supportedSdks() {
        return Arrays.asList(Constants.QISKIT);
    }

    @Override
    public List<String> getLanguagesForSdk(String sdkName) {
        if (sdkName.equals(Constants.QISKIT)) {
            return Arrays.asList(Constants.QISKIT, Constants.OPENQASM);
        }

        return null;
    }

    @Override
    public List<String> supportedProviders() {
        return Arrays.asList(Constants.IBMQ, Constants.IONQ);
    }

    @Override
    public Set<Parameter> getSdkSpecificParameters() {
        // only the token is required
        return new HashSet<>(Arrays.asList(
            new Parameter(Constants.TOKEN_PARAMETER, DataType.String, null, "Parameter for Qiskit SDK Plugin")));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName().toLowerCase().replace("sdkconnector", "");
    }

    public List<String> getSupportedLanguages() {
        return Arrays.asList(Constants.QISKIT, Constants.OPENQASM);
    }

    @Override
    public OriginalCircuitInformation getOriginalCircuitProperties(File circuit, String language) {
        LOG.debug("Retrieving circuit properties for original circuit passed as file with language '{}'.", language);
        try {
            // retrieve content form file and encode base64
            String fileContent = FileUtils.readFileToString(circuit, StandardCharsets.UTF_8);
            String encodedCircuit = Base64.getEncoder().encodeToString(fileContent.getBytes());
            Map<String, ParameterValue> emptyMap = Collections.emptyMap();
            QiskitRequest request = new QiskitRequest(encodedCircuit, emptyMap, language);
            return executeOriginalCircuitPropertiesRequest(request);
        } catch (IOException e) {
            LOG.error("Unable to read file content from circuit file!");
        }
        return null;
    }
}
