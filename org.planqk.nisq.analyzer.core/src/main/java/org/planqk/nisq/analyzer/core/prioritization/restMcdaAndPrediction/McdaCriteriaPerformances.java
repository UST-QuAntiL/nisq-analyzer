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

package org.planqk.nisq.analyzer.core.prioritization.restMcdaAndPrediction;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class McdaCriteriaPerformances {

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private float histogramIntersection;

    @Getter
    @Setter
    @JsonProperty("width")
    private int analyzedWidth;

    @Getter
    @Setter
    @JsonProperty("depth")
    private int analyzedDepth;

    @Getter
    @Setter
    @JsonProperty("multi-qubit-gate-depth")
    private int analyzedMultiQubitGateDepth;

    @Getter
    @Setter
    @JsonProperty("total-number-of-operations")
    private int analyzedTotalNumberOfOperations;

    @Getter
    @Setter
    @JsonProperty("number-of-single-qubit-gates")
    private int analyzedNumberOfSingleQubitGates;

    @Getter
    @Setter
    @JsonProperty("number-of-multi-qubit-gates")
    private int analyzedNumberOfMultiQubitGates;

    @Getter
    @Setter
    @JsonProperty("number-of-measurement-operations")
    private int analyzedNumberOfMeasurementOperations;

    @Getter
    @Setter
    @JsonProperty("avg-single-qubit-gate-error")
    private float avgSingleQubitGateError;

    @Getter
    @Setter
    @JsonProperty("avg-multi-qubit-gate-error")
    private float avgMultiQubitGateError;

    @Getter
    @Setter
    @JsonProperty("avg-single-qubit-gate-time")
    private float avgSingleQubitGateTime;

    @Getter
    @Setter
    @JsonProperty("avg-multi-qubit-gate-time")
    private float avgMultiQubitGateTime;

    @Getter
    @Setter
    @JsonProperty("avg-readout-error")
    private float avgReadoutError;

    @Getter
    @Setter
    @JsonProperty("avg-t1")
    private float t1;

    @Getter
    @Setter
    @JsonProperty("avg-t2")
    private float t2;

    @Getter
    @Setter
    @JsonProperty("queue-size")
    private int queueSize;
}
