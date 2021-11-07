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

package org.planqk.nisq.analyzer.core.connector.pytket;

import static org.planqk.nisq.analyzer.core.web.Utils.getBearerTokenFromRefreshToken;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.ExecutionRequestResult;
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
    private URI transpileAPIEndpoint;

    private URI executeAPIEndpoint;

    public PyTketSdkConnector(
            @Value("${org.planqk.nisq.analyzer.connector.pytket.hostname}") String hostname,
            @Value("${org.planqk.nisq.analyzer.connector.pytket.port}") int port,
            @Value("${org.planqk.nisq.analyzer.connector.pytket.version}") String version
    ) {
        // compile the API endpoints
        this.transpileAPIEndpoint = URI.create(String.format("http://%s:%d/pytket-service/api/%s/transpile", hostname, port, version));
        this.executeAPIEndpoint = URI.create(String.format("http://%s:%d/pytket-service/api/%s/execute", hostname, port, version));
    }

    @Override
    public void executeQuantumAlgorithmImplementation(Implementation implementation, Qpu qpu, Map<String, ParameterValue> parameters,
                                                      ExecutionResult executionResult, ExecutionResultRepository resultRepository, String refreshToken) {

        LOG.debug("Executing quantum algorithm implementation with PyTKet Sdk connector plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        PyTketRequest request =
                new PyTketRequest(implementation.getFileLocation(), implementation.getLanguage(), qpu.getName(), qpu.getProvider(), parameters, bearerToken);
        executeQuantumCircuit(request, executionResult, resultRepository, null);
    }

    @Override
    public void executeTranspiledQuantumCircuit(String transpiledCircuit, String transpiledLanguage, String providerName, String qpuName,
                                                Map<String, ParameterValue> parameters, ExecutionResult executionResult,
                                                ExecutionResultRepository resultRepository,
                                                QpuSelectionResultRepository qpuSelectionResultRepository) {
        LOG.debug("Executing circuit passed as file with provider '{}' and qpu '{}'.", providerName, qpuName);

        PyTketRequest request = null;

        switch (transpiledLanguage.toLowerCase()) {
            case "openqasm":
                request = new PyTketRequest(qpuName, parameters, transpiledCircuit, providerName, PyTketRequest.TranspiledLanguage.OpenQASM);
                break;
            case "quil":
                request = new PyTketRequest(qpuName, parameters, transpiledCircuit, providerName, PyTketRequest.TranspiledLanguage.Quil);
                break;
        }

        if (request != null) {
            executeQuantumCircuit(request, executionResult, resultRepository, qpuSelectionResultRepository);
        } else {
            // change the result status
            executionResult.setStatus(ExecutionResultStatus.FAILED);
            executionResult.setStatusCode("Failed to create execution request for provided transpiled language: " + transpiledLanguage);
            resultRepository.save(executionResult);
        }
    }

    private void executeQuantumCircuit(PyTketRequest request, ExecutionResult executionResult, ExecutionResultRepository resultRepository,
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
            while (executionResult.getStatus() != ExecutionResultStatus.FINISHED && executionResult.getStatus() != ExecutionResultStatus.FAILED) {
                try {
                    ExecutionRequestResult result = restTemplate.getForObject(resultLocation, ExecutionRequestResult.class);

                    // Check if execution is completed
                    if (result.isComplete()) {
                        executionResult.setStatus(ExecutionResultStatus.FINISHED);
                        executionResult.setStatusCode("Execution successfully completed.");
                        executionResult.setResult(result.getResult().toString());
                        executionResult.setShots(result.getShots());

                        // histogram intersection
                        //FIXME currently only for qpu-selection
                        Optional<QpuSelectionResult> qpuSelectionResult =
                            qpuSelectionResultRepository.findById(executionResult.getQpuSelectionResult().getId());
                        if (qpuSelectionResult.isPresent()) {
                            // get stored token for the execution
                            QpuSelectionResult qResult = qpuSelectionResult.get();

                            // check if target machine is of Rigetti or IBMQ, consider accordingly qvm simulator or ibmq_qasm_simulator
                            String simulator;
                            if (Objects.equals(qResult.getProvider(), Constants.RIGETTI)) {
                                simulator = "qvm";
                            } else {
                                simulator = "qasm_simulator";
                            }

                            // check if current execution result is already of a simulator otherwise get all qpu-selection-results of same job
                            if (!qResult.getQpu().contains(simulator)) {
                                List<QpuSelectionResult> jobResults =
                                    qpuSelectionResultRepository.findAll().stream()
                                        .filter(res -> res.getQpuSelectionJobId().equals(qResult.getQpuSelectionJobId()))
                                        .collect(Collectors.toList());
                                // get qpuSelectionResult of simulator if available
                                QpuSelectionResult simulatorQpuSelectionResult =
                                    jobResults.stream().filter(jobResult -> jobResult.getQpu().contains(simulator)).findFirst().orElse(null);
                                if (Objects.nonNull(simulatorQpuSelectionResult)) {
                                    //check if qpu-selection result of simulator was already executed otherwise wait max 1 minute
                                    int iterator = 60;
                                    while (iterator > 0) {
                                        try {
                                            Thread.sleep(1000);
                                            ExecutionResult simulatorExecutionResult =
                                                resultRepository
                                                    .findAll()
                                                    .stream()
                                                    .filter(exeResult -> exeResult.getQpuSelectionResult().getId()
                                                        .equals(simulatorQpuSelectionResult.getId()))
                                                    .findFirst()
                                                    .orElse(null);

                                            // as soon as execution result of simulator is returned calculate histogram intersection
                                            if (Objects.nonNull(simulatorExecutionResult)) {
                                                // convert stored execution result of simulator to Map
                                                String simulatorExecutionResultString = simulatorExecutionResult.getResult();
                                                Map<String, Integer> simulatorCountsOfResults = new HashMap<>();
                                                String rawData = simulatorExecutionResultString.replaceAll("[\\{\\}\\s+]", "");
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
                                                        Math.min(simulatorCountsOfResults.get(simulatorKey), qpuExecutionResult.get(simulatorKey));
                                                }
                                                if (intersection > 0) {
                                                    executionResult.setHistogramIntersectionValue(intersection / simulatorExecutionResult.getShots());
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
                new PyTketRequest(implementation.getFileLocation(), implementation.getLanguage(), qpuName, providerName, parameters, bearerToken);
        return executeCircuitPropertiesRequest(request);
    }

    @Override
    public CircuitInformation getCircuitProperties(File circuit, String language, String providerName, String qpuName,
                                                   Map<String, ParameterValue> parameters) {
        LOG.debug("Retrieving circuit properties for circuit passed as file with provider '{}', qpu '{}', and language '{}'.", providerName, qpuName,
                language);
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
            ResponseEntity<CircuitInformation> response = restTemplate.postForEntity(transpileAPIEndpoint, request, CircuitInformation.class);

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
        return Arrays.asList(Constants.IBMQ, Constants.RIGETTI);
    }

    @Override
    public Set<Parameter> getSdkSpecificParameters() {
        // only the token is required
        return new HashSet<>(Arrays.asList(new Parameter(TOKEN_PARAMETER, DataType.String, null, "Parameter for Qiskit SDK Plugin")));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName().toLowerCase().replace("sdkconnector", "");
    }
}
