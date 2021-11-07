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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Object to encapsulate all information that is retrieved during the analysis of a quantum circuit.
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CircuitInformation {

    @Getter
    @Setter
    @JsonProperty("depth")
    private int circuitDepth = 0;

    @Getter
    @Setter
    @JsonProperty("width")
    private int circuitWidth = 0;

    @Getter
    @Setter
    @JsonProperty("total-number-of-operations")
    private int circuitTotalNumberOfOperations = 0;

    @Getter
    @Setter
    @JsonProperty("number-of-single-qubit-gates")
    private int circuitNumberOfSingleQubitGates = 0;

    @Getter
    @Setter
    @JsonProperty("number-of-multi-qubit-gates")
    private int circuitNumberOfMultiQubitGates = 0;

    @Getter
    @Setter
    @JsonProperty("number-of-measurement-operations")
    private int circuitNumberOfMeasurementOperations = 0;

    @Getter
    @Setter
    @JsonProperty("multi-qubit-gate-depth")
    private int circuitMultiQubitGateDepth = 0;

    @Getter
    @Setter
    @JsonAlias({"transpiled-qasm", "transpiled-quil"})
    private String transpiledCircuit;

    @Getter
    @Setter
    @JsonProperty("language")
    private String transpiledLanguage;

    @Getter
    @Setter
    private String error;

    /**
     * Returns whether the transpilation was successfull
     * @return
     */
    public boolean wasTranspilationSuccessfull()
    {
        return this.error == null;
    }
}
