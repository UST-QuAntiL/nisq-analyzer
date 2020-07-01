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
import java.util.Map;

import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    final private static String qiskitServiceHostname = "127.0.0.1";
    final private static int port = 5000;
    final private static URI transpileAPIEndpoint = URI.create(String.format("http://%s:%d/qiskit-service/api/v1.0/transpile", qiskitServiceHostname, port));
    final private static URI executeAPIEndpoint = URI.create(String.format("http://%s:%d/qiskit-service/api/v1.0/execute", qiskitServiceHostname, port));

    @Override
    public void executeQuantumAlgorithmImplementation(URL algorithmImplementationURL, Qpu qpu, Map<String, String> parameters, ExecutionResult executionResult) {
        LOG.debug("Executing quantum algorithm implementation with Qiskit Sdk connector plugin!");

        // TODO: call Qiskit service, change status to running, wait for results/errors and change status/content of result object

        // Prepare the request
        RestTemplate restTemplate = new RestTemplate();
        String token = qpu.getProvider().getSecretKey();
        QiskitRequest request = new QiskitRequest(algorithmImplementationURL, qpu.getName(), parameters, token);

        try
        {
            // make the execution request
            URI resultLocation = restTemplate.postForLocation(executeAPIEndpoint, request);

            // change the result status
            executionResult.setStatus(ExecutionResultStatus.RUNNING);
            executionResult.setStatusCode("Pending for execution on Qiskit Service ...");

            // poll the Qiskit service frequently
            while (executionResult.getStatus() != ExecutionResultStatus.FINISHED ||  executionResult.getStatus() != ExecutionResultStatus.FAILED)
            {
                try
                {
                    QiskitExecutionResult result = restTemplate.getForObject(resultLocation, QiskitExecutionResult.class);

                    // Check if execution is completed
                    if (result.isComplete())
                    {
                        executionResult.setStatus(ExecutionResultStatus.FINISHED);
                        executionResult.setStatusCode("Execution successfully completed.");
                        executionResult.setResult(result.getResult());
                    }

                    // Wait a second for next poll
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        // pass
                    }

                }
                catch (RestClientException e)
                {
                    LOG.error("Polling result from Qiskit Service failed.");
                    executionResult.setStatus(ExecutionResultStatus.FAILED);
                    executionResult.setStatusCode("Polling result from Qiskit Service failed.");
                }

            }

        }
        catch (RestClientException e)
        {
            LOG.error("Connection to Qiskit Service failed.");
            executionResult.setStatus(ExecutionResultStatus.FAILED);
            executionResult.setStatusCode("Connection to Qiskit Service failed.");
        }
    }

    @Override
    public CircuitInformation getCircuitProperties(URL algorithmImplementationURL, Qpu qpu, Map<String, String> parameters) {
        LOG.debug("Analysing quantum algorithm implementation with Qiskit Sdk connector plugin!");

        // Build the payload for the request
        RestTemplate restTemplate = new RestTemplate();
        String token = qpu.getProvider().getSecretKey();
        QiskitRequest request = new QiskitRequest(algorithmImplementationURL, qpu.getName(), parameters, token);

        try
        {
            // Transpile the given algorithm implementation using Qiskit service
            ResponseEntity<CircuitInformation> response = restTemplate.postForEntity(transpileAPIEndpoint, request, CircuitInformation.class);

            // Check if the Qiskit service was successful
            if (response.getStatusCode().is2xxSuccessful())
            {
                LOG.debug("Circuit transpiled using Qiskit Service.");
                return response.getBody();
            }
            else if (response.getStatusCode().is4xxClientError())
            {
                LOG.error(String.format("Qiskit Service rejected request (HTTP %d)", response.getStatusCodeValue()));
            }
            else if (response.getStatusCode().is5xxServerError())
            {
                LOG.error(String.format("Internal Qiskit Service error (HTTP %d)", response.getStatusCodeValue()));
            }
        }
        catch (RestClientException e)
        {
            LOG.error("Connection to Qiskit Service failed.");
        }

        // Return default Circuit on failure ?
        return new CircuitInformation(1, 1);
    }

    @Override
    public String supportedSdk() {
        return "Qiskit";
    }
}
