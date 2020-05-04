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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.planqk.atlas.core.model.Algorithm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.lang.NonNull;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;

/**
 * Data transfer object for Algorithms ({@link org.planqk.atlas.core.model.Algorithm}).
 */
@ToString(callSuper = true, includeFieldNames = true)
public class AlgorithmDto extends RepresentationModel<AlgorithmDto> {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String name;

    @Setter
    private ParameterListDto inputParameters;

    @Setter
    private ParameterListDto outputParameters;

    @Getter
    @Setter
    // we do not embedded tags into the object (via @jsonInclude) - instead, we add a hateoas link to the associated tags
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    // annotate this for swagger as well, because swagger doesn't recognize the json property annotation
    @Schema(accessMode = WRITE_ONLY)
    private Set<TagDto> tags;

    @Setter
    @Getter
    private Object content;

    public AlgorithmDto() {
    }

    @NonNull
    public ParameterListDto getInputParameters() {
        if (Objects.isNull(inputParameters)) {
            return new ParameterListDto();
        }
        return inputParameters;
    }

    @NonNull
    public ParameterListDto getOutputParameters() {
        if (Objects.isNull(outputParameters)) {
            return new ParameterListDto();
        }
        return outputParameters;
    }

    public static final class Converter {

        public static AlgorithmDto convert(final Algorithm object) {
            final AlgorithmDto dto = new AlgorithmDto();
            dto.setId(object.getId());
            dto.setName(object.getName());
            dto.setContent(object.getContent());
            dto.setTags(object.getTags().stream().map(TagDto.Converter::convert).collect(Collectors.toSet()));
            ParameterListDto inputParams = new ParameterListDto();
            inputParams.add(object.getInputParameters().stream().map(ParameterDto.Converter::convert)
                    .collect(Collectors.toList()));
            dto.setInputParameters(inputParams);

            ParameterListDto outputParams = new ParameterListDto();
            outputParams.add(object.getOutputParameters().stream().map(ParameterDto.Converter::convert)
                    .collect(Collectors.toList()));
            dto.setOutputParameters(outputParams);

            return dto;
        }

        public static Algorithm convert(final AlgorithmDto object) {
            final Algorithm algo = new Algorithm();
            algo.setName(object.getName());
            algo.setContent(object.getContent());
            if (Objects.nonNull(object.getTags())) {
                algo.setTags(object.getTags().stream().map(TagDto.Converter::convert).collect(Collectors.toSet()));
            }
            algo.setInputParameters(object.getInputParameters().getParameters().stream()
                    .map(ParameterDto.Converter::convert)
                    .collect(Collectors.toList()));
            algo.setOutputParameters(object.getOutputParameters().getParameters().stream()
                    .map(ParameterDto.Converter::convert)
                    .collect(Collectors.toList()));
            return algo;
        }
    }
}
