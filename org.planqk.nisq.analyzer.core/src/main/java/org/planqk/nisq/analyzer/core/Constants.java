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

package org.planqk.nisq.analyzer.core;

/**
 * Constants for the Quality API classes.
 */
public class Constants {

    // URL snippets
    public static final String IMPLEMENTATIONS = "implementations";
    public static final String QPUS = "qpus";
    public static final String SDKS = "sdks";
    public static final String EXECUTION = "execute";
    public static final String SELECTION = "selection";
    public static final String COMPILER_SELECTION = "compiler-selection";
    public static final String SELECTION_PARAMS = "selection-params";
    public static final String RESULTS = "results";
    public static final String COMPILER_RESULTS = "compiler-results";

    // link names
    public static final String USED_SDK = "used-sdk";
    public static final String SUPPORTED_SDK = "supported-sdk";
    public static final String INPUT_PARAMS = "input-parameters";
    public static final String OUTPUT_PARAMS = "output-parameters";
    public static final String RESULTS_LINK = "results";
    public static final String EXECUTED_ALGORITHM_LINK = "executed-algorithm";
    public static final String USED_QPU_LINK = "used-qpu";
    public static final String USED_ANALYSIS_RESULT = "analysis-result";

    // circuit languages
    public static final String OPENQASM = "OpenQASM";
    public static final String QUIL = "Quil";
    public static final String PYQUIL = "pyQuil";

    // provider
    public static final String IBMQ = "ibmq";

    // sdks
    public static final String QISKIT = "Qiskit";
    public static final String PYTKET = "pytket";

    // parameter
    public static final String TOKEN_PARAMETER = "token";
}
