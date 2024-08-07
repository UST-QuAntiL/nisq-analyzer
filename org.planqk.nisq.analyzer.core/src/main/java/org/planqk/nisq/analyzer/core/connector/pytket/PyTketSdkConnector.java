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

package org.planqk.nisq.analyzer.core.connector.pytket;

import static org.planqk.nisq.analyzer.core.connector.ConnectorUtils.calculateHistogramIntersection;
import static org.planqk.nisq.analyzer.core.web.Utils.getBearerTokenFromRefreshToken;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
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
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PyTketSdkConnector implements SdkConnector {

    final private static Logger LOG = LoggerFactory.getLogger(PyTketSdkConnector.class);

    final private static String TOKEN_PARAMETER = "token";

    @Value("${org.planqk.nisq.analyzer.connector.pytket.pollInterval:10000}")
    private int pollInterval;

    // API Endpoints
    private URI generateAPIEndpoint;

    private URI transpileAPIEndpoint;

    private URI executeAPIEndpoint;

    private URI analyzeOriginalAPIEndpoint;

    public PyTketSdkConnector(@Value("${org.planqk.nisq.analyzer.connector.pytket.hostname}") String hostname,
                              @Value("${org.planqk.nisq.analyzer.connector.pytket.port}") int port,
                              @Value("${org.planqk.nisq.analyzer.connector.pytket.version}") String version) {
        // compile the API endpoints
        generateAPIEndpoint =
            URI.create(String.format("http://%s:%d/pytket-service/api/%s/generate-circuit", hostname, port, version));
        analyzeOriginalAPIEndpoint = URI.create(
            String.format("http://%s:%d/pytket-service/api/%s/analyze-original-circuit", hostname, port, version));
        this.transpileAPIEndpoint =
            URI.create(String.format("http://%s:%d/pytket-service/api/%s/transpile", hostname, port, version));
        this.executeAPIEndpoint =
            URI.create(String.format("http://%s:%d/pytket-service/api/%s/execute", hostname, port, version));
    }

    public CircuitInformationOfImplementation getCircuitOfImplementation(Implementation implementation,
                                                                         Map<String, ParameterValue> parameters,
                                                                         String refreshToken) {
        LOG.debug(
            "Generating and analyzing quantum circuit of quantum algorithm implementation with Pytket Sdk connector " +
                "plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        PyTketRequest request =
            new PyTketRequest(implementation.getFileLocation(), implementation.getLanguage(), parameters, bearerToken);

        RestTemplate restTemplate = new RestTemplate();
        try {
            // request to generate circuit
            URI circuitLocation = restTemplate.postForLocation(generateAPIEndpoint, request);

            ExecutionResultStatus generationComplete = ExecutionResultStatus.RUNNING;

            // poll the Pytket service frequently
            while (generationComplete != ExecutionResultStatus.FAILED) {
                try {
                    ResponseEntity<CircuitInformationOfImplementation> response =
                        restTemplate.exchange(circuitLocation, HttpMethod.GET, null,
                            CircuitInformationOfImplementation.class);

                    CircuitInformationOfImplementation result = null;

                    // Check if the Pytket service was successful
                    if (response.getStatusCode().is2xxSuccessful()) {
                        LOG.debug("Generating circuit using Pytket Service.");
                        result = response.getBody();
                        if (implementation.getLanguage().equalsIgnoreCase("qiskit")) {
                            result.setCircuitLanguage(Constants.OPENQASM);
                        } else if (implementation.getLanguage().equalsIgnoreCase("pyquil")) {
                            result.setCircuitLanguage(Constants.QUIL);
                        }

                        // Check if generation is completed
                        if (result.isComplete()) {
                            generationComplete = ExecutionResultStatus.FINISHED;
                            return result;
                        }
                    } else if (response.getStatusCode().is4xxClientError()) {
                        LOG.error(
                            String.format("Pytket Service rejected request (HTTP %d)", response.getStatusCodeValue()));
                        generationComplete = ExecutionResultStatus.FAILED;
                    } else if (response.getStatusCode().is5xxServerError()) {
                        LOG.error(
                            String.format("Internal Pytket Service error (HTTP %d)", response.getStatusCodeValue()));
                        generationComplete = ExecutionResultStatus.FAILED;
                    }

                    // Wait for next poll
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e) {
                        // pass
                    }
                } catch (RestClientException e) {
                    LOG.error("Polling generation result from Pytket Service failed.");
                    generationComplete = ExecutionResultStatus.FAILED;
                }
            }
        } catch (RestClientException e) {
            LOG.error("Circuit generation with Pytket Service failed.");
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void executeTranspiledQuantumCircuit(String transpiledCircuit, String transpiledLanguage,
                                                String providerName, String qpuName,
                                                Map<String, ParameterValue> parameters, ExecutionResult executionResult,
                                                ExecutionResultRepository resultRepository,
                                                QpuSelectionResultRepository qpuSelectionResultRepository,
                                                String correlationId, URL fileLocation) {
        LOG.debug("Executing circuit passed as file with provider '{}' and qpu '{}'.", providerName, qpuName);

        PyTketRequest request = null;

        switch (transpiledLanguage.toLowerCase()) {
            case "openqasm":
                request = new PyTketRequest(qpuName, parameters, transpiledCircuit, providerName,
                    PyTketRequest.TranspiledLanguage.OpenQASM, correlationId, fileLocation);
                break;
            case "quil":
                request = new PyTketRequest(qpuName, parameters, transpiledCircuit, providerName,
                    PyTketRequest.TranspiledLanguage.Quil, correlationId, fileLocation);
                break;
        }

        if (request != null) {
            executeQuantumCircuit(request, executionResult, resultRepository, qpuSelectionResultRepository);
        } else {
            // change the result status
            executionResult.setStatus(ExecutionResultStatus.FAILED);
            executionResult.setStatusCode(
                "Failed to create execution request for provided transpiled language: " + transpiledLanguage);
            resultRepository.save(executionResult);
        }
    }

    private void executeQuantumCircuit(PyTketRequest request, ExecutionResult executionResult,
                                       ExecutionResultRepository resultRepository,
                                       QpuSelectionResultRepository qpuSelectionResultRepository) {
        try {
            // make the execution request
            RestTemplate restTemplate = new RestTemplate();
            URI resultLocation = restTemplate.postForLocation(executeAPIEndpoint, request);

            // change the result status
            executionResult.setStatus(ExecutionResultStatus.RUNNING);
            executionResult.setStatusCode("Pending for execution on PyTKet Service ...");
            resultRepository.save(executionResult);

            // poll the PyTKet service frequently
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
                        executionResult.setResultLocation(resultLocation);

                        // histogram intersection
                        //FIXME currently only for qpu-selection
                        if (Objects.nonNull(qpuSelectionResultRepository)) {
                            Optional<QpuSelectionResult> qpuSelectionResult =
                                qpuSelectionResultRepository.findById(executionResult.getQpuSelectionResult().getId());
                            if (qpuSelectionResult.isPresent()) {
                                // get stored token for the execution
                                QpuSelectionResult qResult = qpuSelectionResult.get();

                                // check if target machine is of Rigetti or IBMQ, consider accordingly qvm simulator
                                // or ibmq simulator
                                String simulator;
                                if (Objects.equals(qResult.getProvider(), Constants.RIGETTI)) {
                                    simulator = "qvm";
                                } else {
                                    simulator = "simulator";
                                }

                                // check if current execution result is already of a simulator otherwise get all
                                // qpu-selection-results of same job
                                calculateHistogramIntersection(executionResult, resultRepository,
                                    qpuSelectionResultRepository, result, qResult, simulator);
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
                    LOG.error("Polling result from PyTKet Service failed.");
                    executionResult.setStatus(ExecutionResultStatus.FAILED);
                    executionResult.setStatusCode("Polling result from PyTKet Service failed.");
                    resultRepository.save(executionResult);
                }
            }
        } catch (RestClientException e) {
            LOG.error("Connection to PyTKet Service failed.");
            executionResult.setStatus(ExecutionResultStatus.FAILED);
            executionResult.setStatusCode("Connection to PyTKet Service failed.");
            resultRepository.save(executionResult);
        }
    }

    @Override
    public CircuitInformation getCircuitProperties(Implementation implementation, String providerName, String qpuName,
                                                   Map<String, ParameterValue> parameters, String refreshToken) {
        LOG.debug("Analysing quantum algorithm implementation with PyTket Sdk connector plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        PyTketRequest request =
            new PyTketRequest(implementation.getFileLocation(), implementation.getLanguage(), qpuName, providerName,
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
            PyTketRequest request = new PyTketRequest(encodedCircuit, parameters, language, qpuName, providerName);
            return executeCircuitPropertiesRequest(request);
        } catch (IOException e) {
            LOG.error("Unable to read file content from circuit file!");
        }
        return null;
    }

    private CircuitInformation executeCircuitPropertiesRequest(PyTketRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        try {
            // Transpile the given algorithm implementation using PyTket service
            ResponseEntity<CircuitInformation> response =
                restTemplate.postForEntity(transpileAPIEndpoint, request, CircuitInformation.class);

            // Check if the PyTket service was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.debug("Circuit transpiled using PyTket Service.");
                return response.getBody();
            } else if (response.getStatusCode().is4xxClientError()) {
                LOG.error(String.format("PyTket Service rejected request (HTTP %d)", response.getStatusCodeValue()));
            } else if (response.getStatusCode().is5xxServerError()) {
                LOG.error(String.format("Internal PyTket Service error (HTTP %d)", response.getStatusCodeValue()));
            }
        } catch (RestClientException e) {
            LOG.error("Connection to PyTket Service failed.");
        }

        return null;
    }

    private OriginalCircuitInformation executeOriginalCircuitPropertiesRequest(PyTketRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Analyze the given original circuit using Pytket service
            ResponseEntity<OriginalCircuitInformation> response =
                restTemplate.postForEntity(analyzeOriginalAPIEndpoint, request, OriginalCircuitInformation.class);

            // Check if the Pytket service was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.debug("Original circuit analyzing using Pytket Service.");

                return response.getBody();
            } else if (response.getStatusCode().is4xxClientError()) {
                LOG.error(String.format("Pytket Service rejected request (HTTP %d)", response.getStatusCodeValue()));
            } else if (response.getStatusCode().is5xxServerError()) {
                LOG.error(String.format("Internal Pytket Service error (HTTP %d)", response.getStatusCodeValue()));
            }
        } catch (RestClientException e) {
            LOG.error("Connection to Pytket Service failed.");
        }

        return null;
    }

    @Override
    public List<String> supportedSdks() {
        return Arrays.asList(Constants.PYTKET);
    }

    @Override
    public List<String> getLanguagesForSdk(String sdkName) {
        if (sdkName.equals(Constants.PYTKET)) {
            return Arrays.asList(Constants.QISKIT, Constants.OPENQASM, Constants.PYQUIL);
        }

        return null;
    }

    @Override
    public List<String> supportedProviders() {
        return Arrays.asList(Constants.IBMQ, Constants.RIGETTI, Constants.IONQ);
    }

    @Override
    public Set<Parameter> getSdkSpecificParameters() {
        // only the token is required
        return new HashSet<>(
            Arrays.asList(new Parameter(TOKEN_PARAMETER, DataType.String, null, "Parameter for Qiskit SDK Plugin")));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName().toLowerCase().replace("sdkconnector", "");
    }

    public List<String> getSupportedLanguages() {
        return Arrays.asList(Constants.QISKIT, Constants.OPENQASM, Constants.QUIL);
    }

    @Override
    public OriginalCircuitInformation getOriginalCircuitProperties(File circuit, String language) {
        LOG.debug("Retrieving circuit properties for original circuit passed as file with language '{}'.", language);
        try {
            // retrieve content form file and encode base64
            String fileContent = FileUtils.readFileToString(circuit, StandardCharsets.UTF_8);
            String encodedCircuit = Base64.getEncoder().encodeToString(fileContent.getBytes());
            PyTketRequest request = new PyTketRequest(encodedCircuit, language);
            return executeOriginalCircuitPropertiesRequest(request);
        } catch (IOException e) {
            LOG.error("Unable to read file content from circuit file!");
        }
        return null;
    }
}
