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
import javax.xml.namespace.QName;

/**
 * Class containing the names of the criteria that are supported, as well as if they have to be retrieved from the QPU or the circuit analysis.
 */
public class McdaConstants {

    public static final String AVG_SINGLE_QUBIT_GATE_ERROR = "avg-single-qubit-gate-error";

    public static final String AVG_MULTI_QUBIT_GATE_ERROR = "avg-multi-qubit-gate-error";

    public static final String AVG_SINGLE_QUBIT_GATE_TIME = "avg-single-qubit-gate-time";

    public static final String AVG_MULTI_QUBIT_GATE_TIME = "avg-multi-qubit-gate-time";

    public static final String AVG_READOUT_ERROR = "avg-readout-error";

    public static final String AVG_T1 = "avg-t1";

    public static final String AVG_T2 = "avg-t2";

    public static final String QUEUE_SIZE = "queue-size";

    public static final List<String> QPU_CRITERION =
        Arrays.asList(AVG_SINGLE_QUBIT_GATE_ERROR, AVG_MULTI_QUBIT_GATE_ERROR, AVG_SINGLE_QUBIT_GATE_TIME, AVG_MULTI_QUBIT_GATE_TIME,
            AVG_READOUT_ERROR, AVG_T1, AVG_T2, QUEUE_SIZE);

    public static final String WIDTH = "width";

    public static final String DEPTH = "depth";

    public static final String TOTAL_NUMBER_OF_OPERATIONS = "total-number-of-operations";

    public static final String NUMBER_OF_SINGLE_QUBIT_GATES = "number-of-single-qubit-gates";

    public static final String NUMBER_OF_MULTI_QUBIT_GATES = "number-of-multi-qubit-gates";

    public static final String NUMBER_OF_MEASUREMENT_OPERATIONS = "number-of-measurement-operations";

    public static final String MULTI_QUBIT_GATE_DEPTH = "multi-qubit-gate-depth";

    public static final List<String> CIRCUIT_CRITERION = Arrays
        .asList(WIDTH, DEPTH, TOTAL_NUMBER_OF_OPERATIONS, NUMBER_OF_SINGLE_QUBIT_GATES, NUMBER_OF_MULTI_QUBIT_GATES, NUMBER_OF_MEASUREMENT_OPERATIONS,
            MULTI_QUBIT_GATE_DEPTH);

    // ***** MCDA web services *****
    public static final String WEB_SERVICE_OPERATIONS_INVOKE = "submitProblem";

    public static final String WEB_SERVICE_OPERATIONS_REQUEST_SOLUTION = "requestSolution";

    // XMCDA namespace for the Electre services
    public static final String WEB_SERVICE_NAMESPACE_2_0_0 = "http://www.decision-deck.org/2009/XMCDA-2.0.0";

    // XMCDA namespace for the promethee services
    public static final String WEB_SERVICE_NAMESPACE_2_1_0 = "http://www.decision-deck.org/2009/XMCDA-2.1.0";

    // XMCDA namspace for the TOPSIS services
    public static final String WEB_SERVICE_NAMESPACE_2_2_2 = "http://www.decision-deck.org/2017/XMCDA-2.2.2";

    // default namespace used by the XMCDA library
    public static final String WEB_SERVICE_NAMESPACE_DEFAULT = "http://www.decision-deck.org/2019/XMCDA-2.2.3";

    public static final String WEB_SERVICE_DATA_CRITERIA = "criteria";
    public static final String WEB_SERVICE_DATA_ALTERNATIVES = "alternatives";
    public static final String WEB_SERVICE_DATA_PERFORMANCE = "performance";
    public static final String WEB_SERVICE_DATA_FLOW_TYPE = "flow_type";
    public static final String WEB_SERVICE_DATA_PERFORMANCES = "performances";
    public static final String WEB_SERVICE_DATA_PREFERENCE = "preference";
    public static final String WEB_SERVICE_DATA_FLOWS = "flows";
    public static final String WEB_SERVICE_DATA_WEIGHTS = "weights";
    public static final String WEB_SERVICE_DATA_TICKET = "ticket";
    public static final String WEB_SERVICE_DATA_STATUS = "service-status";
    public static final String WEB_SERVICE_DATA_MESSAGES = "messages";
    public static final String WEB_SERVICE_DATA_IDEAL_POSITIVE = "ideal_positive";
    public static final String WEB_SERVICE_DATA_IDEAL_NEGATIVE = "ideal_negative";
    public static final String WEB_SERVICE_DATA_SCORES = "scores";
    public static final String WEB_SERVICE_DATA_CONCORDANCE = "concordance";
    public static final String WEB_SERVICE_DATA_DISCORDANCES = "discordances";
    public static final String WEB_SERVICE_DATA_OUTRANKING = "outranking";
    public static final String WEB_SERVICE_DATA_RELATION = "relation";
    public static final String WEB_SERVICE_DATA_OPTIONS = "options";
    public static final String WEB_SERVICE_DATA_OUTPUT_RELATION = "output_relation";
    public static final String WEB_SERVICE_DATA_CUT_TYPE = "cut_type";
    public static final String WEB_SERVICE_DATA_CUT_THRESHOLD = "cut_threshold";
    public static final String WEB_SERVICE_DATA_CLASSICAL_OUTPUT = "classical_output";
    public static final String WEB_SERVICE_DATA_OUTRANKING_RELATION = "outrankingRelation";
    public static final String WEB_SERVICE_DATA_INTERSECTION_DISTILLATION = "intersectionDistillation";

    public static final String WEB_SERVICE_NAME_TOPSIS_RANKING = "TOPSIS_ranking-PUT.py";

    public static final String WEB_SERVICE_NAME_TOPSIS_ALTERNATIVES = "TOPSIS_extremeAlternatives-PUT.py";

    public static final String WEB_SERVICE_NAME_TOPSIS_WEIGHTING = "TOPSIS_normalizationAndWeighting-PUT.py";

    public static final String WEB_SERVICE_NAME_ELECTREIII_CONCORDANCE = "ElectreConcordance-J-MCDA.py";

    public static final String WEB_SERVICE_NAME_ELECTREIII_DISCORDANCE = "ElectreDiscordances-J-MCDA.py";

    public static final String WEB_SERVICE_NAME_ELECTREIII_OUTRANKING = "ElectreOutranking-J-MCDA.py";

    public static final String WEB_SERVICE_NAME_ELECTREIII_RELATION = "cutRelation-ITTB.py";

    public static final String WEB_SERVICE_NAME_ELECTREIII_RANKING = "alternativesRankingViaQualificationDistillation-ITTB.py";

    public static final String WEB_SERVICE_NAME_PROMETHEEII_PREFERENCE = "PrometheePreference-J-MCDA.py";

    public static final String WEB_SERVICE_NAME_PROMETHEEII_FLOWS = "PrometheeFlows-J-MCDA.py";

    public static final QName WEB_SERVICE_QNAMES_LABEL = new QName("", "label");

    public static final QName WEB_SERVICE_QNAMES_VALUE = new QName("", "value");

    public static final QName WEB_SERVICE_QNAMES_PARAMETER = new QName("", "parameter");

    public static final QName WEB_SERVICE_QNAMES_ID = new QName("", "id");

    public static final QName WEB_SERVICE_QNAMES_REAL = new QName("", "real");
}
