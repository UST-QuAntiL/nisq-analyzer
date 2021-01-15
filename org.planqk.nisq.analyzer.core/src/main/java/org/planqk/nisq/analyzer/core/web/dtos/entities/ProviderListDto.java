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

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.planqk.nisq.analyzer.core.model.Provider;

@NoArgsConstructor
@AllArgsConstructor
class EmbeddedProviderListDto {

    @Getter
    @Setter
    @JsonProperty("providerDtoes")
    private List<ProviderDto> providers;
}

@NoArgsConstructor
@AllArgsConstructor
public class ProviderListDto {

    @Getter
    @Setter
    @JsonProperty("_embedded")
    private EmbeddedProviderListDto embedded;

    public List<ProviderDto> getProviders() {
        return this.embedded.getProviders();
    }

    public static final class Converter {
        public static ProviderListDto convert(List<Provider> providers) {

            ProviderListDto dto = new ProviderListDto();

            new EmbeddedProviderListDto(providers.stream().map(ProviderDto.Converter::convert).collect(Collectors.toList()));

            return dto;
        }

        public static List<Provider> convert(ProviderListDto dto) {
            return dto.getProviders().stream().map(ProviderDto.Converter::convert).collect(Collectors.toList());
        }
    }
}
