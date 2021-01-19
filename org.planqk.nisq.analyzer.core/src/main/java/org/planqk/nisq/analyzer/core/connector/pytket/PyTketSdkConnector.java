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

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.planqk.nisq.analyzer.core.connector.CircuitInformation;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.connector.qiskit.QiskitRequest;
import org.planqk.nisq.analyzer.core.connector.qiskit.QiskitSdkConnector;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
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

@Service
public class PyTketSdkConnector implements SdkConnector {

    final private static Logger LOG = LoggerFactory.getLogger(QiskitSdkConnector.class);

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
    public void executeQuantumAlgorithmImplementation(URL algorithmImplementationURL, Qpu qpu, Map<String, ParameterValue> parameters, ExecutionResult executionResult, ExecutionResultRepository resultService) {

    }

    @Override
    public CircuitInformation getCircuitProperties(Implementation implementation, Qpu qpu, Map<String, ParameterValue> parameters) {
        LOG.debug("Analysing quantum algorithm implementation with PyTket Sdk connector plugin!");

        // Build the payload for the request
        RestTemplate restTemplate = new RestTemplate();
        PyTketRequest request = new PyTketRequest(implementation.getFileLocation(), qpu.getName(), qpu.getProvider(), implementation.getSdk().getName(), parameters);

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
        return Arrays.asList(
                "Qiskit",
                "Cirq"
        );
    }

    @Override
    public List<String> supportedProviders() {
        return Arrays.asList(
                "IBMQ",
                "Braket"
        );
    }

    @Override
    public Set<Parameter> getSdkSpecificParameters() {
        // only the token is required
        return new HashSet<>(Arrays.asList(new Parameter(TOKEN_PARAMETER, DataType.String, null, "Parameter for Qiskit SDK Plugin")));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName().toLowerCase();
    }
}
