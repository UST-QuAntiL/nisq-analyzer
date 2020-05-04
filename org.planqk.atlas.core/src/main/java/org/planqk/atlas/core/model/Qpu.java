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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

/**
 * Entity representing a quantum processing unit (Qpu).
 */
@Entity
public class Qpu extends HasId {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private int qubitCount;

    @Getter
    @Setter
    private float t1;

    @Getter
    @Setter
    private float maxGateTime;

    @Setter
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "qpu_sdk",
            joinColumns = @JoinColumn(name = "qpu_id"),
            inverseJoinColumns = @JoinColumn(name = "sdk_id"))
    private List<Sdk> supportedSdks;

    @Getter
    @Setter
    @ManyToOne
    private Provider provider;

    public Qpu() {
    }

    @NonNull
    public List<Sdk> getSupportedSdks() {
        if (Objects.isNull(supportedSdks)) {
            return new ArrayList<>();
        }
        return supportedSdks;
    }
}
