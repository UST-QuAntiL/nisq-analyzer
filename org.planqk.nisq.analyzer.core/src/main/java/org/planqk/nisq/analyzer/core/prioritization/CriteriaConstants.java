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

package org.planqk.nisq.analyzer.core.prioritization;

import java.util.Arrays;
import java.util.List;

/**
 * Class containing the names of the criteria that are supported, as well as if they have to be retrieved from the QPU or the circuit analysis.
 */
public class CriteriaConstants {

    public static final String QUANTUM_COLUME = "quantum-volume";

    public static final String AVG_CNOT_ERROR = "avg-cnot-error";

    public static final String AVG_READOUT_ERROR = "avg-readout-error";

    public static final String AVG_T1 = "avg-T1";

    public static final List<String> QPU_CRITERION = Arrays.asList(QUANTUM_COLUME, AVG_CNOT_ERROR, AVG_READOUT_ERROR, AVG_T1);

    public static final String WIDTH = "width";

    public static final String DEPTH = "depth";

    public static final String NUMBER_OF_GATES = "number-of-gates";

    public static final String NUMBER_OF_MUTLI_QUBIT_GATES = "number-of-multi-qubit-gates";

    public static final List<String> CIRCUIT_CRITERION = Arrays.asList(WIDTH, DEPTH, NUMBER_OF_GATES, NUMBER_OF_MUTLI_QUBIT_GATES);
}
