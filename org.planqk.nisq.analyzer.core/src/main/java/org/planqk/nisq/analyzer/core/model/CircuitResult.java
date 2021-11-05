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

package org.planqk.nisq.analyzer.core.model;

import java.time.OffsetDateTime;
import javax.persistence.MappedSuperclass;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Base class defining the properties for all circuits related to analyses
 */
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public abstract class CircuitResult extends HasId {

    @Getter
    @Setter
    private String provider;

    @Getter
    @Setter
    private String qpu;

    @Getter
    @Setter
    private String compiler;

    @Getter
    @Setter
    private String circuitName;

    @Getter
    @Setter
    private int analyzedDepth;

    @Getter
    @Setter
    private int analyzedWidth;

    @Getter
    @Setter
    private int analyzedTotalNumberOfOperations;

    @Getter
    @Setter
    private int analyzedNumberOfMultiQubitGates;

    @Getter
    @Setter
    private int analyzedMultiQubitGateDepth;

    @Getter
    @Setter
    private OffsetDateTime time;
}
