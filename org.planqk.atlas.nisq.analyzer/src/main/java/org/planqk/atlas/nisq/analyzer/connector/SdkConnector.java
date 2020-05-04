/*******************************************************************************
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

/**
 * Interface for the interaction with a certain SDK.
 */
public interface SdkConnector {

    /**
     * Execute the given quantum algorithm implementation with the given input parameters.
     *
     * @param algorithmImplementationURL the URL to the file containing the quantum algorithm implementation that should
     *                                   be executed
     * @param qpu                        the QPU to execute the implementation on
     * @param parameters                 the input parameters for the quantum algorithm execution
     * @param executionResult            the object to update the current state of the long running task and to add the
     *                                   results after completion
     */
    void executeQuantumAlgorithmImplementation(URL algorithmImplementationURL, Qpu qpu, Map<String, String> parameters, ExecutionResult executionResult);

    /**
     * Analyse the quantum algorithm implementation located at the given URL after transpiling it for the given QPU and
     * with the given input parameters.
     *
     * @param algorithmImplementationURL the URL to the file containing the quantum algorithm implementation that should
     *                                   be analyzed
     * @param qpu                        the QPU to analyze the implementation for
     * @param parameters                 he input parameters for the quantum algorithm implementation
     * @return the object containing all analysed properties of the quantum circuit
     */
    CircuitInformation getCircuitProperties(URL algorithmImplementationURL, Qpu qpu, Map<String, String> parameters);

    /**
     * Returns the name of the Sdk that is supported by the connector
     *
     * @return the name of the supported SDK
     */
    String supportedSdk();
}
