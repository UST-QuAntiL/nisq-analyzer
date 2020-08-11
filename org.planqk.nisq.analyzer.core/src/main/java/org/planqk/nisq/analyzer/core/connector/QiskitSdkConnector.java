/********************************************************************************
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

package org.planqk.nisq.analyzer.core.connector;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    final private static String TOKEN_PARAMETER = "token";

    @Value("${org.planqk.nisq.analyzer.connector.qiskit.pollInterval:10000}")
    private int pollInterval;

    // API Endpoints
    private URI transpileAPIEndpoint;
    private URI executeAPIEndpoint;

    public QiskitSdkConnector(
            @Value("${NISQ_HOSTNAME}") String hostname,
            @Value("${NISQ_PORT}") int port,
            @Value("${NISQ_VERSION}") String version
    ) {
        // compile the API endpoints
        transpileAPIEndpoint = URI.create(String.format("http://%s:%d/qiskit-service/api/%s/transpile", hostname, port, version));
        executeAPIEndpoint = URI.create(String.format("http://%s:%d/qiskit-service/api/%s/execute", hostname, port, version));
    }

    @Override
    public void executeQuantumAlgorithmImplementation(URL algorithmImplementationURL, Qpu qpu, Map<String, ParameterValue> parameters, ExecutionResult executionResult, ExecutionResultRepository resultRepository) {
        LOG.debug("Executing quantum algorithm implementation with Qiskit Sdk connector plugin!");

        String token = getTokenFromInputParameters(ParameterValue.convertToUntyped(parameters));
        if (Objects.isNull(token)) {
            LOG.error("Required token parameter not provided!");
            return;
        }

        // Prepare the request
        RestTemplate restTemplate = new RestTemplate();
        QiskitRequest request = new QiskitRequest(algorithmImplementationURL, qpu.getName(), parameters, token);

        try {
            // make the execution request
            URI resultLocation = restTemplate.postForLocation(executeAPIEndpoint, request);

            // change the result status
            executionResult.setStatus(ExecutionResultStatus.RUNNING);
            executionResult.setStatusCode("Pending for execution on Qiskit Service ...");
            resultRepository.save(executionResult);

            // poll the Qiskit service frequently
            while (executionResult.getStatus() != ExecutionResultStatus.FINISHED && executionResult.getStatus() != ExecutionResultStatus.FAILED) {
                try {
                    QiskitExecutionResult result = restTemplate.getForObject(resultLocation, QiskitExecutionResult.class);

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

    @Override
    public CircuitInformation getCircuitProperties(URL algorithmImplementationURL, Qpu qpu, Map<String, ParameterValue> parameters) {
        LOG.debug("Analysing quantum algorithm implementation with Qiskit Sdk connector plugin!");

        String token = getTokenFromInputParameters(ParameterValue.convertToUntyped(parameters));
        if (Objects.isNull(token)) {
            LOG.error("Required token parameter not provided!");
            return null;
        }

        // Build the payload for the request
        RestTemplate restTemplate = new RestTemplate();
        QiskitRequest request = new QiskitRequest(algorithmImplementationURL, qpu.getName(), parameters, token);

        try {
            // Transpile the given algorithm implementation using Qiskit service
            ResponseEntity<CircuitInformation> response = restTemplate.postForEntity(transpileAPIEndpoint, request, CircuitInformation.class);

            // Check if the Qiskit service was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.debug("Circuit transpiled using Qiskit Service.");
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
    public String supportedSdk() {
        return "Qiskit";
    }

    @Override
    public Set<Parameter> getSdkSpecificParameters() {
        // only the token is required
        return new HashSet<>(Arrays.asList(new Parameter(TOKEN_PARAMETER, DataType.String, null, "Parameter for Qiskit SDK Plugin")));
    }

    /**
     * Retrieve token parameter from input parameters
     *
     * @param parameters the set of input parameters
     * @return the value of the token parameter
     */
    private String getTokenFromInputParameters(Map<String, String> parameters) {
        if (parameters.containsKey(TOKEN_PARAMETER)) {
            return parameters.get(TOKEN_PARAMETER);
        }
        return null;
    }
}
