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
import javax.persistence.OneToMany;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Entity representing a quantum hardware provider.
 */
@Entity
public class Provider extends HasId {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String accessKey;

    @Getter
    @Setter
    private String secretKey;

    @Setter
    @OneToMany(mappedBy = "provider", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Qpu> supportedQpus;

    public Provider() {
    }

    @NonNull
    public List<Qpu> getSupportedQpus() {
        if (Objects.isNull(supportedQpus)) {
            return new ArrayList<>();
        }
        return supportedQpus;
    }
}
