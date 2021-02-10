/*******************************************************************************
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.NonNull;

/**
 * Entity representing an implementation of a certain quantum algorithm.
 */
@Entity
@NoArgsConstructor
public class Implementation extends HasId {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String selectionRule;

    @Getter
    @Setter
    private URL fileLocation;

    @Getter
    @Setter
    private String language;

    @Getter
    @Setter
    private UUID implementedAlgorithm;

    @Getter
    @Setter
    @ManyToOne
    private Sdk sdk;

    @Setter
    @OneToMany(cascade = CascadeType.PERSIST)
    private List<ExecutionResult> executionResults;

    @Setter
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Parameter> inputParameters;

    @Setter
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Parameter> outputParameters;

    @NonNull
    public List<Parameter> getInputParameters() {
        if (Objects.isNull(inputParameters)) {
            return new ArrayList<>();
        }
        return inputParameters;
    }

    @NonNull
    public List<Parameter> getOutputParameters() {
        if (Objects.isNull(outputParameters)) {
            return new ArrayList<>();
        }
        return outputParameters;
    }

    @NonNull
    public List<ExecutionResult> getExecutionResults() {
        if (Objects.isNull(executionResults)) {
            return new ArrayList<>();
        }
        return executionResults;
    }
}
