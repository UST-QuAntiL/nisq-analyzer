/*******************************************************************************
 * Copyright (c) 2022 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.prioritization.restMcdaAndPrediction.preSelectionModel;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PreSelectionPredictionRequest {
    private String compilerPropertyName = "compiler";

    private String histogramIntersectionName = "histogramIntersection";

    private String queueSizeName = "queue-size";

    private Float queueSizeImportance = 0.0f;

    private List<String> inputMetricNames =
        Arrays.asList("original-width", "original-depth", "original-multi-qubit-gate-depth", "original-total-number-of-operations",
            "original-number-of-single-qubit-gates", "original-number-of-multi-qubit-gates", "original-number-of-measurement-operations",
            "avg-single-qubit-gate-error", "avg-multi-qubit-gate-error", "avg-single-qubit-gate-time", "avg-multi-qubit-gate-time",
            "avg-readout-error", "avg-t1", "avg-t2");

    private String machineLearningMethod;

    private String metaRegressor;

    private List<TrainingData> trainingData;

    private NewCircuit newCircuit;
}
