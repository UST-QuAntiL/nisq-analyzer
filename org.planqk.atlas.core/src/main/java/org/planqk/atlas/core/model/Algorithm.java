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
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import lombok.Setter;
import org.springframework.lang.NonNull;

/**
 * Entity representing a quantum algorithm, e.g., Shors factorization algorithm.
 */
@Entity
public class Algorithm extends AlgorOrImpl {

    @ManyToMany(mappedBy = "implementedAlgorithm", cascade =
            {CascadeType.MERGE})
    @Setter
    private Set<Implementation> implementations;

    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "algorithm_tag",
            joinColumns = @JoinColumn(name = "algorithm_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Setter
    private Set<Tag> tags;

    public Algorithm() {
        super();
    }

    @NonNull
    public Set<Implementation> getImplementations() {
        if (Objects.isNull(implementations)) {
            return new HashSet<>();
        }
        return implementations;
    }

    @NonNull
    public Set<Tag> getTags() {
        if (Objects.isNull(tags)) {
            return new HashSet<>();
        }
        return tags;
    }
}
