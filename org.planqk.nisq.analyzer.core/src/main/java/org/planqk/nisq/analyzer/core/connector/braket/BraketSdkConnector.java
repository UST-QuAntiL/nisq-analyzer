/********************************************************************************
 * Copyright (c) 2022 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.connector.braket;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.ExecutionRequestResult;
import org.planqk.nisq.analyzer.core.connector.OriginalCircuitInformation;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.planqk.nisq.analyzer.core.web.Utils.getBearerTokenFromRefreshToken;

/**
 * Sdk connector which passes execution and analysis requests to a connected Forest service.
 */
@Service
public class BraketSdkConnector implements SdkConnector {

    final private static Logger LOG = LoggerFactory.getLogger(BraketSdkConnector.class);

    @Value("${org.planqk.nisq.analyzer.connector.qsharp.pollInterval:10000}")
    private int pollInterval;

    // API Endpoints
    private URI transpileAPIEndpoint;

    private URI executeAPIEndpoint;

    public BraketSdkConnector(
            @Value("${org.planqk.nisq.analyzer.connector.braket.hostname}") String hostname,
            @Value("${org.planqk.nisq.analyzer.connector.braket.port}") int port,
            @Value("${org.planqk.nisq.analyzer.connector.braket.version}") String version
    ) {
        // compile the API endpoints
        transpileAPIEndpoint = URI.create(String.format("http://%s:%d/braket-service/api/%s/transpile", hostname, port, version));
        executeAPIEndpoint = URI.create(String.format("http://%s:%d/braket-service/api/%s/execute", hostname, port, version));
    }

    @Override
    public void executeQuantumAlgorithmImplementation(Implementation implementation, Qpu qpu, Map<String, ParameterValue> parameters,
                                                      ExecutionResult executionResult, ExecutionResultRepository resultRepository, String refreshToken) {
        LOG.debug("Executing quantum algorithm implementation with Braket Sdk connector plugin!");
        String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
        BraketRequest request = new BraketRequest(implementation.getFileLocation(), implementation.getLanguage(), qpu.getName(), parameters, bearerToken);
        executeQuantumCircuit(request, executionResult, resultRepository);
    }

    @Override
    public void executeTranspiledQuantumCircuit(String transpiledCircuit, String transpiledLanguage, String providerName, String qpuName,
                                                Map<String, ParameterValue> parameters, ExecutionResult executionResult,
                                                ExecutionResultRepository resultRepository,
                                                QpuSelectionResultRepository qpuSelectionResultRepository) {
        LOG.debug("Executing circuit passed as file with provider '{}' and qpu '{}'.", providerName, qpuName);
        BraketRequest request = new BraketRequest(transpiledCircuit, qpuName, parameters);
        executeQuantumCircuit(request, executionResult, resultRepository);
    }

    private void executeQuantumCircuit(BraketRequest request, ExecutionResult executionResult, ExecutionResultRepository resultRepository) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // make the execution request
            URI resultLocation = restTemplate.postForLocation(executeAPIEndpoint, request);

            // change the result status
            executionResult.setStatus(ExecutionResultStatus.RUNNING);
            executionResult.setStatusCode("Pending for execution on Braket Service ...");
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
                        executionResult.setShots(result.getShots());
                        resultRepository.save(executionResult);
                    }

                    // Wait for next poll
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e) {
                        // pass
                    }
                } catch (RestClientException e) {
                    LOG.error("Polling result from Braket Service failed.");
                    executionResult.setStatus(ExecutionResultStatus.FAILED);
                    executionResult.setStatusCode("Polling result from Braket Service failed.");
                    resultRepository.save(executionResult);
                }
            }
        } catch (RestClientException e) {
            LOG.error("Connection to Braket Service failed.");
            executionResult.setStatus(ExecutionResultStatus.FAILED);
            executionResult.setStatusCode("Connection to Braket Service failed.");
            resultRepository.save(executionResult);
        }
    }

    @Override
    public CircuitInformation getCircuitProperties(Implementation implementation, String providerName, String qpuName,
                                                   Map<String, ParameterValue> parameters, String refreshToken) {
        LOG.error("Braket Service does not support transpilation before execution.");
        return null;
    }

    @Override
    public CircuitInformation getCircuitProperties(File circuit, String language, String providerName, String qpuName,
                                                   Map<String, ParameterValue> parameters) {
        LOG.error("Braket Service does not support transpilation before execution.");
        return null;
    }

    private CircuitInformation executeCircuitPropertiesRequest(BraketRequest request) {
        LOG.error("Braket Service does not support transpilation before execution.");
        return null;
    }

    @Override
    public List<String> supportedSdks() {
        return Arrays.asList(Constants.BRAKET);
    }

    @Override
    public List<String> getLanguagesForSdk(String sdkName) {
        if (sdkName.equals(Constants.BRAKET)) {
            return Arrays.asList(Constants.BRAKET_IR, Constants.BRAKET_SDK);
        }

        return null;
    }

    @Override
    public List<String> supportedProviders() {
        return Arrays.asList(Constants.AMAZON);
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

    @Override
    public List<String> getSupportedLanguages() {
        return Arrays.asList(Constants.BRAKET_IR, Constants.BRAKET_SDK);
    }

    @Override
    public OriginalCircuitInformation getOriginalCircuitProperties(File circuit, String language) {
        //TODO
        return null;
    }
}