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
public class McdaConstants {

    public static final String QUANTUM_VOLUME = "quantum-volume";

    public static final String AVG_MULTI_QUBIT_GATE_ERROR = "avg-multi-qubit-gate-error";

    public static final String AVG_MULTI_QUBIT_GATE_TIME = "avg-multi-qubit-gate-time";

    public static final String AVG_READOUT_ERROR = "avg-readout-error";

    public static final String AVG_T1 = "avg-t1";

    public static final String AVG_T2 = "avg-t2";

    public static final String QUEUE_SIZE = "queue-size";

    public static final List<String> QPU_CRITERION = Arrays.asList(QUANTUM_VOLUME, AVG_MULTI_QUBIT_GATE_ERROR, AVG_MULTI_QUBIT_GATE_TIME,
        AVG_READOUT_ERROR, AVG_T1, AVG_T2, QUEUE_SIZE);

    public static final String WIDTH = "width";

    public static final String DEPTH = "depth";

    public static final String NUMBER_OF_GATES = "number-of-gates";

    public static final String NUMBER_OF_MULTI_QUBIT_GATES = "number-of-multi-qubit-gates";

    public static final String MULTI_QUBIT_GATE_DEPTH = "multi-qubit-gate-depth";

    public static final List<String> CIRCUIT_CRITERION = Arrays
        .asList(WIDTH, DEPTH, NUMBER_OF_GATES, NUMBER_OF_MULTI_QUBIT_GATES, MULTI_QUBIT_GATE_DEPTH);

    // ***** MCDA web services *****
    public static final String WEB_SERVICE_OPERATIONS_INVOKE = "submitProblem";
    public static final String WEB_SERVICE_OPERATIONS_REQUEST_SOLUTION = "requestSolution";

    public static final String WEB_SERVICE_DATA_CRITERIA = "criteria";
    public static final String WEB_SERVICE_DATA_ALTERNATIVES = "alternatives";
    public static final String WEB_SERVICE_DATA_PERFORMANCE = "performance";
    public static final String WEB_SERVICE_DATA_WEIGHTS = "weights";
    public static final String WEB_SERVICE_DATA_TICKET = "ticket";
    public static final String WEB_SERVICE_DATA_STATUS = "service-status";
    public static final String WEB_SERVICE_DATA_MESSAGES = "messages";
    public static final String WEB_SERVICE_DATA_IDEAL_POSITIVE = "ideal_positive";
    public static final String WEB_SERVICE_DATA_IDEAL_NEGATIVE = "ideal_negative";
    public static final String WEB_SERVICE_DATA_SCORES = "scores";

    public static final String WEB_SERVICE_NAME_TOPSIS_RANKING = "TOPSIS_ranking-PUT.py";
    public static final String WEB_SERVICE_NAME_TOPSIS_ALTERNATIVES = "TOPSIS_extremeAlternatives-PUT.py";
    public static final String WEB_SERVICE_NAME_TOPSIS_WEIGHTING = "TOPSIS_normalizationAndWeighting-PUT.py";

    public static final String WEB_SERVICE_NAME_ELECTREIII_CONCORDANCE = "ElectreConcordance-J-MCDA.py";
    public static final String WEB_SERVICE_NAME_ELECTREIII_DISCORDANCE = "ElectreDiscordances-J-MCDA.py";
    public static final String WEB_SERVICE_NAME_ELECTREIII_OUTRANKING = "ElectreOutranking-J-MCDA.py";
    public static final String WEB_SERVICE_NAME_ELECTREIII_RELATION = "cutRelation-ITTB.py";
    public static final String WEB_SERVICE_NAME_ELECTREIII_RANKING = "alternativesRankingViaQualificationDistillation-ITTB.py";
    public static final String WEB_SERVICE_NAME_ELECTREIII_PLOT = "plotAlternativesComparisons-ITTB.py";

    public static final String WEB_SERVICE_NAME_PROMOTHEEI_PREFERENCE = "PrometheePreference-J-MCDA.py";
    public static final String WEB_SERVICE_NAME_PROMOTHEEI_FLOWS = "PrometheeFlows-J-MCDA.py";
    public static final String WEB_SERVICE_NAME_PROMOTHEEI_RANKING = "Promethee1Ranking-RXMCDA.py";
    public static final String WEB_SERVICE_NAME_PROMOTHEEI_PLOT = "plotAlternativesComparisons-ITTB.py";
}
