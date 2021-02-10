/*******************************************************************************
 * Copyright (c) 2021 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.web.dtos.entities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.springframework.hateoas.RepresentationModel;

@NoArgsConstructor
public class ProviderDto extends RepresentationModel<ProviderDto> {

    @Getter
    @Setter
    private UUID id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String offeringURL;


    public static final class Converter {

        public static ProviderDto convert(Provider object) {
            ProviderDto dto = new ProviderDto();
            dto.setId(object.getId());
            dto.setName(object.getName());
            dto.setOfferingURL(object.getOfferingURL().toString());
            return dto;
        }

        public static Provider convert(ProviderDto dto) {
            Provider object = new Provider();
            object.setName(dto.getName());
            object.setId(dto.getId());

            try {
                object.setOfferingURL(new URL(dto.getOfferingURL()));
            } catch (MalformedURLException e) {
                object.setOfferingURL(null);
            }

            return object;
        }
    }
}
