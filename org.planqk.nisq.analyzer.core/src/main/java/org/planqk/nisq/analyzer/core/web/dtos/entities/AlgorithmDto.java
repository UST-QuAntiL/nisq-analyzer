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

package org.planqk.nisq.analyzer.core.web.dtos.entities;

import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.planqk.nisq.analyzer.core.model.Algorithm;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.lang.NonNull;

/**
 * Data transfer object for Algorithms ({@link org.planqk.nisq.analyzer.core.model.Algorithm}).
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
