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

package org.planqk.nisq.analyzer.core.prioritization.restMcda;

import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class McdaRankRestRequest {

    @Getter
    @Setter
    private String mcdaMethod;

    @Getter
    @Setter
    private Map<String, McdaCriterionWeight> metricWeights;

    @Getter
    @Setter
    private Map<String, McdaCriterionWeight> bordaCountMetrics;

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<McdaCompiledCircuitJob> circuits;
}
