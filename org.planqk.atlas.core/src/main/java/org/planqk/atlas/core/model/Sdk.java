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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.NonNull;

/**
 * Entity representing a Sdk to define quantum algorithm {@link Implementation}s.
 */
@Entity
@ToString
public class Sdk extends HasId {

    @Getter
    @Setter
    @Column(unique = true)
    private String name;

    @Setter
    @ManyToMany(mappedBy = "supportedSdks", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private List<Qpu> supportedQpus;

    public Sdk() {
    }

    public void addSupportedQpu(Qpu qpu) {
        supportedQpus.add(qpu);
        qpu.getSupportedSdks().add(this);
    }

    @NonNull
    public List<Qpu> getSupportedQpus() {
        if (Objects.isNull(supportedQpus)) {
            return new ArrayList<>();
        }
        return supportedQpus;
    }
}
