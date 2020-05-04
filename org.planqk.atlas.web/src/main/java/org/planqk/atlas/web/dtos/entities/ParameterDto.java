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

package org.planqk.atlas.web.dtos.entities;

import org.planqk.atlas.core.model.DataType;
import org.planqk.atlas.core.model.Parameter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Data transfer object for {@link Parameter}.
 */
@ToString(callSuper = true, includeFieldNames = true)
public class ParameterDto {

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    DataType type;

    @Getter
    @Setter
    String restriction;

    @Getter
    @Setter
    String description;

    public ParameterDto() {
    }

    public static final class Converter {

        public static ParameterDto convert(final Parameter object) {
            final ParameterDto dto = new ParameterDto();
            dto.setName(object.getName());
            dto.setType(object.getType());
            dto.setRestriction(object.getRestriction());
            dto.setDescription(object.getDescription());
            return dto;
        }

        public static Parameter convert(final ParameterDto object) {
            final Parameter param = new Parameter();
            param.setName(object.getName());
            param.setType(object.getType());
            param.setRestriction(object.getRestriction());
            param.setDescription(object.getDescription());
            return param;
        }
    }
}
