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

package org.planqk.atlas.core.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import lombok.Getter;
import lombok.Setter;

@Entity
public class Tag extends HasId {

    @Getter
    @Setter
    String key;

    @Getter
    @Setter
    String value;

    @ManyToMany(mappedBy = "tags", cascade =
            {CascadeType.MERGE, CascadeType.PERSIST})
    @Setter
    private Set<Algorithm> algorithms;

    @ManyToMany(mappedBy = "tags", cascade =
            {CascadeType.MERGE, CascadeType.PERSIST})
    @Setter
    private Set<Implementation> implementations;

    public Set<Algorithm> getAlgorithms() {
        if (Objects.isNull(algorithms)) {
            return new HashSet<>();
        }
        return algorithms;
    }

    public Set<Implementation> getImplementations() {
        if (Objects.isNull(implementations)) {
            return new HashSet<>();
        }
        return implementations;
    }

}
