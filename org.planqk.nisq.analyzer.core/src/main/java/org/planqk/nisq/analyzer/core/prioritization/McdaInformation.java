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

import org.xmcda.v2.XMCDA;

import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper containing all information about a job required for running MCDA methods to ease handling the different kinds of jobs
 */
public class McdaInformation {

    @Getter
    @Setter
    XMCDA alternatives;

    @Getter
    @Setter
    XMCDA performances;

    // TODO: add required data for running MCDA methods
}
