/********************************************************************************
 * Copyright (c) 2020 University of Stuttgart
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing the result of an execution of a quantum algorithm implementation on a certain QPU.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionResult extends HasId {

    @Getter
    @Setter
    private ExecutionResultStatus status;

    @Getter
    @Setter
    private String statusCode;

    @Getter
    @Setter
    @ManyToOne
    private AnalysisResult analysisResult;

    @Getter
    @Setter
    @ManyToOne
    private CompilationResult compilationResult;

    @Getter
    @Setter
    @ManyToOne
    private QpuSelectionResult qpuSelectionResult;

    @Getter
    @Setter
    @Column(columnDefinition = "text")
    private String result;

    @Getter
    @Setter
    private int shots;

    @Getter
    @Setter
    @ManyToOne
    private Implementation executedImplementation;
}
