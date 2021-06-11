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

package org.planqk.nisq.analyzer.core.connector.forest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.ExecutionRequestResult;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.planqk.nisq.analyzer.core.web.Utils.getBearerTokenFromRefreshToken;

/**
 * Sdk connector which passes execution and analysis requests to a connected Forest service.
 */
@Service
public class ForestSdkConnector implements SdkConnector {

    final private static Logger LOG = LoggerFactory.getLogger(ForestSdkConnector.class);

    @Value("${org.planqk.nisq.analyzer.connector.forest.pollInterval:10000}")
    private int pollInterval;

    // API Endpoints
    private URI transpileAPIEndpoint;

    private URI executeAPIEndpoint;

    public ForestSdkConnector(
            @Value("${org.planqk.nisq.analyzer.connector.forest.hostname}") String hostname,
            @Value("${org.planqk.nisq.analyzer.connector.forest.port}") int port,
            @Value("${org.planqk.nisq.analyzer.connector.forest.version}") String version
    ) {
        // compile the API endpoints
        transpileAPIEndpoint = URI.create(String.format("http://%s:%d/forest-service/api/%s/transpile", hostname, port, version));
        executeAPIEndpoint = URI.create(String.format("http://%s:%d/forest-service/api/%s/execute", hostname, port, version));
    }

    @Override
    public void executeQuantumAlgorithmImplementation(Implementation implementation, Qpu qpu, Map<String, ParameterValue> parameters,
                                                      ExecutionResult executionResult, ExecutionResultRepository resultRepository, String refreshToken) {
        LOG.debug("Executing quantum algorithm implementation with Forest Sdk connector plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        ForestRequest request = new ForestRequest(implementation.getFileLocation(), implementation.getLanguage(), qpu.getName(), parameters, bearerToken);
        executeQuantumCircuit(request, executionResult, resultRepository);
    }

    @Override
    public void executeTranspiledQuantumCircuit(String transpiledCircuit, String transpiledLanguage, String providerName, String qpuName,
                                                Map<String, ParameterValue> parameters, ExecutionResult executionResult,
                                                ExecutionResultRepository resultRepository) {
        LOG.debug("Executing circuit passed as file with provider '{}' and qpu '{}'.", providerName, qpuName);
        ForestRequest request = new ForestRequest(transpiledCircuit, qpuName, parameters);
        executeQuantumCircuit(request, executionResult, resultRepository);
    }

    private void executeQuantumCircuit(ForestRequest request, ExecutionResult executionResult, ExecutionResultRepository resultRepository) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // make the execution request
            URI resultLocation = restTemplate.postForLocation(executeAPIEndpoint, request);

            // change the result status
            executionResult.setStatus(ExecutionResultStatus.RUNNING);
            executionResult.setStatusCode("Pending for execution on Forest Service ...");
            resultRepository.save(executionResult);

            // poll the Forest service frequently
            while (executionResult.getStatus() != ExecutionResultStatus.FINISHED && executionResult.getStatus() != ExecutionResultStatus.FAILED) {
                try {
                    ExecutionRequestResult result = restTemplate.getForObject(resultLocation, ExecutionRequestResult.class);

                    // Check if execution is completed
                    if (result.isComplete()) {
                        executionResult.setStatus(ExecutionResultStatus.FINISHED);
                        executionResult.setStatusCode("Execution successfully completed.");
                        executionResult.setResult(result.getResult().toString());
                        resultRepository.save(executionResult);
                    }

                    // Wait for next poll
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e) {
                        // pass
                    }
                } catch (RestClientException e) {
                    LOG.error("Polling result from Forest Service failed.");
                    executionResult.setStatus(ExecutionResultStatus.FAILED);
                    executionResult.setStatusCode("Polling result from Forest Service failed.");
                    resultRepository.save(executionResult);
                }
            }
        } catch (RestClientException e) {
            LOG.error("Connection to Forest Service failed.");
            executionResult.setStatus(ExecutionResultStatus.FAILED);
            executionResult.setStatusCode("Connection to Forest Service failed.");
            resultRepository.save(executionResult);
        }
    }

    @Override
    public CircuitInformation getCircuitProperties(Implementation implementation, String providerName, String qpuName,
                                                   Map<String, ParameterValue> parameters, String refreshToken) {
        LOG.debug("Analysing quantum algorithm implementation with Forest Sdk connector plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        ForestRequest request = new ForestRequest(implementation.getFileLocation(), implementation.getLanguage(), qpuName, parameters, bearerToken);
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
            ForestRequest request = new ForestRequest(language, encodedCircuit, qpuName, parameters);
            return executeCircuitPropertiesRequest(request);
        } catch (IOException e) {
            LOG.error("Unable to read file content from circuit file!");
        }
        return null;
    }

    private CircuitInformation executeCircuitPropertiesRequest(ForestRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Transpile the given algorithm implementation using Forest service
            ResponseEntity<CircuitInformation> response = restTemplate.postForEntity(transpileAPIEndpoint, request, CircuitInformation.class);

            // Check if the Forest service was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.debug("Circuit transpiled using Forest Service.");
                CircuitInformation circuitInformation = response.getBody();

                // update language required for selection
                if (Objects.nonNull(circuitInformation)) {
                    circuitInformation.setTranspiledLanguage(Constants.QUIL);
                }
                return circuitInformation;
            } else if (response.getStatusCode().is4xxClientError()) {
                LOG.error(String.format("Forest Service rejected request (HTTP %d)", response.getStatusCodeValue()));
            } else if (response.getStatusCode().is5xxServerError()) {
                LOG.error(String.format("Internal Forest Service error (HTTP %d)", response.getStatusCodeValue()));
            }
        } catch (RestClientException e) {
            LOG.error("Connection to Forest Service failed.");
        }

        return null;
    }

    @Override
    public List<String> supportedSdks() {
        return Arrays.asList(Constants.FOREST);
    }

    @Override
    public List<String> getLanguagesForSdk(String sdkName) {
        if (sdkName.equals(Constants.FOREST)) {
            return Arrays.asList(Constants.PYQUIL, Constants.QUIL);
        }

        return null;
    }

    @Override
    public List<String> supportedProviders() {
        return Arrays.asList(Constants.RIGETTI);
    }

    @Override
    public Set<Parameter> getSdkSpecificParameters() {
        // no special parameters required
        return new HashSet<>();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName().toLowerCase().replace("sdkconnector", "");
    }
}
