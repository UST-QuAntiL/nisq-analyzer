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

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class SensitivityAnalysisResultResponse {

    @Getter
    @Setter
    @JsonProperty("original_scores")
    private ArrayList<Float> originalScores;

    @Getter
    @Setter
    @JsonProperty("original_ranking")
    private ArrayList<Integer> originalRanking;

    @Getter
    @Setter
    @JsonProperty("original_borda_count_ranking")
    private ArrayList<Integer> originalBordaCountRanking;

    @Getter
    @Setter
    @JsonProperty("decreasing_factors")
    private ArrayList<Float> decreasingFactors;

    @Getter
    @Setter
    @JsonProperty("disturbed_ranks_decreased")
    private ArrayList<ArrayList<Integer>> disturbedRanksDecreased;

    @Getter
    @Setter
    @JsonProperty("disturbed_borda_ranks_decreased")
    private ArrayList<ArrayList<ArrayList<Integer>>> disturbedBordaRanksDecreased;

    @Getter
    @Setter
    @JsonProperty("increasing_factors")
    private ArrayList<Float> increasingFactors;

    @Getter
    @Setter
    @JsonProperty("disturbed_ranks_increased")
    private ArrayList<ArrayList<Integer>> disturbedRanksIncreased;

    @Getter
    @Setter
    @JsonProperty("disturbed_borda_ranks_increased")
    private ArrayList<ArrayList<ArrayList<Integer>>> disturbedBordaRanksIncreased;
}
