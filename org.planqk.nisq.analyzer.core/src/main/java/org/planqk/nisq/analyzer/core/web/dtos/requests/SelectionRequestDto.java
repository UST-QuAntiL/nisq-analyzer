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

package org.planqk.nisq.analyzer.core.web.dtos.requests;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SelectionRequestDto extends ParameterKeyValueDto {

    List<String> allowedProviders;

    List<String> compilers;

    boolean preciseResultsPreference;

    boolean shortWaitingTimesPreference;

    Float queueImportanceRatio;

    int maxNumberOfCompiledCircuits;

    String predictionAlgorithm;

    String metaOptimizer;

    Map<String, Map<String, String>> tokens;

    private UUID algorithmId;

    private String refreshToken;

    private String mcdaMethodName;

    private String mcdaWeightLearningMethod;
}
