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
 * Constants for the NISQ Analyzer API classes.
 */
public class Constants {

    // URL snippets
    public static final String IMPLEMENTATIONS = "implementations";
    public static final String SDKS = "sdks";
    public static final String EXECUTION = "execute";

    public static final String SELECTION = "selection";

    public static final String COMPILER_SELECTION = "compiler-selection";

    public static final String QPU_SELECTION = "qpu-selection";

    public static final String SELECTION_PARAMS = "selection-params";

    public static final String ANALYSIS_RESULTS = "analysis-results";

    public static final String COMPILER_RESULTS = "compiler-results";

    public static final String QPU_SELECTION_RESULTS = "qpu-selection-results";

    public static final String EXECUTION_RESULTS = "execution-results";

    public static final String JOBS = "jobs";

    public static final String MCDA_METHODS = "mcda-methods";

    public static final String WEIGHT_LEARNING_METHODS = "weight-learning-methods";

    public static final String CRITERIA = "criteria";

    public static final String CRITERIA_VALUE = "value";

    public static final String MCDA_PRIORITIZE = "prioritize";

    public static final String MCDA_LEARN_WEIGHTS = "learning-weights";

    public static final String MCDA_ANALYZE_SENSITIVITY = "analyze-sensitivity";

    public static final String MCDA_SENSITIVITY_ANALYZES = "sensitivity-analyzes";

    public static final String COMPILERS = "compilers";

    // link names
    public static final String USED_SDK = "used-sdk";

    public static final String INPUT_PARAMS = "input-parameters";

    public static final String OUTPUT_PARAMS = "output-parameters";

    public static final String RESULTS_LINK = "results";

    public static final String EXECUTED_ALGORITHM_LINK = "executed-algorithm";
    public static final String USED_ANALYSIS_RESULT = "analysis-result";
    public static final String USED_COMPILATION_RESULT = "compilation-result";
    public static final String USED_QPU_SELECTION_RESULT = "qpu-selection-result";

    // circuit languages
    public static final String OPENQASM = "openqasm";
    public static final String QUIL = "quil";
    public static final String PYQUIL = "pyquil";
    public static final String CIRQ_JSON = "cirq-json";
    public static final String QSHARP = "qsharp";
    public static final String BRAKET = "braket";


    // provider
    public static final String IBMQ = "ibmq";
    public static final String RIGETTI = "rigetti";
    public static final String GOOGLE = "cirq-google";

    // sdks
    public static final String QISKIT = "qiskit";
    public static final String PYTKET = "pytket";
    public static final String FOREST = "forest";
    public static final String CIRQ = "cirq";

    // parameter
    public static final String TOKEN_PARAMETER = "token";
}
