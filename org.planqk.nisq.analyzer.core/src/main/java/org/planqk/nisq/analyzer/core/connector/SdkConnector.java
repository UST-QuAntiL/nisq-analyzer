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

package org.planqk.nisq.analyzer.core.connector;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;

/**
 * Interface for the interaction with a certain SDK.
 */
public interface SdkConnector {

    /**
     * Execute the given quantum algorithm implementation with the given input parameters.
     *
     * @param implementation the implementation that should be executed
     * @param qpu            the QPU to execute the implementation on
     * @param parameters     the input parameters for the quantum algorithm execution
     * @param resultService  the object to update the current state of the long running task and to add the results
     *                       after completion
     * @param refreshToken   a valid refresh token from the PlanQK platform, only needs to be specified if the
     *                       implementation is hosted on the PlanQK platform
     */
    void executeQuantumAlgorithmImplementation(Implementation implementation, Qpu qpu,
                                               Map<String, ParameterValue> parameters, ExecutionResult executionResult,
                                               ExecutionResultRepository resultService, String refreshToken);

    /**
     * Execute the given transpiled quantum circuit.
     *
     * @param transpiledCircuit  the transpiled circuit that should be executed
     * @param transpiledLanguage the language the circuit is transpiled in
     * @param providerName       the provider name for the QPU to execute the circuit
     * @param qpuName            the name of the QPU to execute the circuit
     * @param parameters         the set of parameters for the execution, inlcuding the access token if required
     * @param executionResult    the object to store the result
     * @param resultRepository   the object to update the current state of the long running task and to add the results
     *                           after completion
     */
    void executeTranspiledQuantumCircuit(String transpiledCircuit, String transpiledLanguage, String providerName,
                                         String qpuName, Map<String, ParameterValue> parameters,
                                         ExecutionResult executionResult, ExecutionResultRepository resultRepository,
                                         QpuSelectionResultRepository qpuSelectionResultRepository);

    /**
     * Analyse the quantum algorithm implementation located at the given URL after compiling it for the given QPU and
     * with the given input parameters.
     *
     * @param implementation the implementation to get the circuit properties for
     * @param providerName   the name of the provider of the QPU
     * @param qpuName        the name of the QPU to analyze the implementation for
     * @param parameters     he input parameters for the quantum algorithm implementation
     * @return the object containing all analysed properties of the quantum circuit
     */
    CircuitInformation getCircuitProperties(Implementation implementation, String providerName, String qpuName,
                                            Map<String, ParameterValue> parameters, String refreshToken);

    /**
     * Analyse the given circuit after compiling it for the given QPU and with the given input parameters.
     *
     * @param circuit      the file containing the circuit
     * @param language     the language of the circuit
     * @param providerName the name of the provider of the QPU
     * @param qpuName      the name of the QPU to analyze the implementation for
     * @param parameters   he input parameters for the quantum algorithm implementation
     * @return the object containing all analysed properties of the quantum circuit
     */
    CircuitInformation getCircuitProperties(File circuit, String language, String providerName, String qpuName,
                                            Map<String, ParameterValue> parameters);

    /**
     * Returns the names of the Sdks that are supported by the connector
     *
     * @return the names of the supported SDKs
     */
    List<String> supportedSdks();

    /**
     * Returns the list of supported languages for the given SDK
     *
     * @param sdkName the name of the SDK
     * @return the list of languages that can be understood by the SDK
     */
    List<String> getLanguagesForSdk(String sdkName);

    /**
     * Returns the names of the providers that are supported by the connector
     *
     * @return the names of the supported providers
     */
    List<String> supportedProviders();

    /**
     * Get parameters which are required by the SDK to execute a quantum circuit and which are independent of
     * problem-specific input data
     *
     * @return a Set of required parameters
     */
    Set<Parameter> getSdkSpecificParameters();

    /**
     * Returns the unique name of the implemented SDK connector
     *
     * @return
     */
    String getName();

    /**
     * Returns the natively supported languages of the SDK
     *
     * @return
     */
    List<String> getSupportedLanguages();

    /**
     * Analyse the given original, non-transpiled circuit.
     *
     * @param circuit  the file containing the circuit
     * @param language the language of the circuit
     * @return the object containing all analysed properties of the original quantum circuit
     */
    OriginalCircuitInformation getOriginalCircuitProperties(File circuit, String language);

    /**
     * Get the generated circuit of the given implementation for the specific input parameters and the analyzed
     * properties of this circuit.
     *
     * @param implementation the implementation from which the circuit should be generated
     * @param parameters     the input parameters for the implementation
     * @return the object containing all analysed properties of the original quantum circuit and the circuit itself
     */
    CircuitInformationOfImplementation getCircuitOfImplementation(Implementation implementation,
                                                                  Map<String, ParameterValue> parameters,
                                                                  String refreshToken);
}
