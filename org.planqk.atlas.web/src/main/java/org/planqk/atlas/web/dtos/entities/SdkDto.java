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

import org.planqk.atlas.core.model.Sdk;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;

@ToString(callSuper = true, includeFieldNames = true)
public class SdkDto extends RepresentationModel<SdkDto> {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String name;

    public static final class Converter {

        public static SdkDto convert(final Sdk object) {
            final SdkDto dto = new SdkDto();
            dto.setId(object.getId());
            dto.setName(object.getName());
            return dto;
        }

        public static Sdk convert(final SdkDto object) {
            final Sdk sdk = new Sdk();
            sdk.setName(object.getName());
            return sdk;
        }
    }
}
