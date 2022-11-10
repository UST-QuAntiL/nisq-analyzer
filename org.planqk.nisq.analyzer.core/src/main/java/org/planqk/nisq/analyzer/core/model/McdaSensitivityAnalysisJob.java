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

package org.planqk.nisq.analyzer.core.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Object to represent a MCDA sensitivity analysis job running the sensitivity analysis for a certain analysis or compilation job
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McdaSensitivityAnalysisJob extends Job {

    private String method;

    private String state;

    private boolean useBordaCount;

    @ElementCollection
    private Map<String, Float> bordaCountWeights;

    private UUID jobId;

    private JobType jobType;

    private float stepSize;

    private float upperBound;

    private float lowerBound;

    private String plotFileLocation;

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<McdaResult> originalRanking;
}
