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

package org.planqk.atlas.nisq.analyzer.connector;

import java.net.URL;
import java.util.Map;

import org.planqk.atlas.core.model.ExecutionResult;
import org.planqk.atlas.core.model.Qpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sdk connector which passes execution and analysis requests to a connected Qiskit service.
 */
@Service
public class QiskitSdkConnector implements SdkConnector {

    final private static Logger LOG = LoggerFactory.getLogger(QiskitSdkConnector.class);

    @Override
    public void executeQuantumAlgorithmImplementation(URL algorithmImplementationURL, Qpu qpu, Map<String, String> parameters, ExecutionResult executionResult) {
        LOG.debug("Executing quantum algorithm implementation with Qiskit Sdk connector plugin!");

        // TODO: call Qiskit service, change status to running, wait for results/errors and change status/content of result object
    }

    @Override
    public CircuitInformation getCircuitProperties(URL algorithmImplementationURL, Qpu qpu, Map<String, String> parameters) {
        LOG.debug("Analysing quantum algorithm implementation with Qiskit Sdk connector plugin!");

        // TODO: call Qiskit service to analyse given circuit
        return new CircuitInformation(1, 1);
    }

    @Override
    public String supportedSdk() {
        return "Qiskit";
    }
}
