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
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.lang.NonNull;

@MappedSuperclass
@TypeDef(
        name = "jsonb",
        typeClass = JsonBinaryType.class
)
public abstract class AlgorOrImpl extends HasId {

    @Getter
    @Setter
    private String name;

    @Setter
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Parameter> inputParameters;

    @Setter
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Parameter> outputParameters;

    @Getter
    @Setter
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Object content;

    public AlgorOrImpl() {
    }

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
}
