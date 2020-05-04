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

import org.planqk.atlas.core.model.Provider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;

/**
 * Data transfer object for the model class {@link Provider}.
 */
@ToString(callSuper = true, includeFieldNames = true)
public class ProviderDto extends RepresentationModel<ProviderDto> {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String accessKey;

    @Getter
    @Setter
    private String secretKey;

    public static final class Converter {

        public static ProviderDto convert(final Provider object) {
            ProviderDto dto = new ProviderDto();
            dto.setId(object.getId());
            dto.setName(object.getName());
            dto.setAccessKey(object.getAccessKey());
            dto.setSecretKey("******"); // do not show password in API
            return dto;
        }

        public static Provider convert(final ProviderDto object) {
            Provider provider = new Provider();
            provider.setName(object.getName());
            provider.setAccessKey(object.getAccessKey());
            provider.setSecretKey(object.getSecretKey());
            return provider;
        }
    }
}
