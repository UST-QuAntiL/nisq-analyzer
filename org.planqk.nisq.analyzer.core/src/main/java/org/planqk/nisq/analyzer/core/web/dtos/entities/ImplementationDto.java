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

import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.lang.NonNull;

/**
 * Data transfer object for the model class Implementation ({@link org.planqk.nisq.analyzer.core.model.Implementation}).
 */
@ToString(callSuper = true, includeFieldNames = true)
@NoArgsConstructor
public class ImplementationDto extends RepresentationModel<ImplementationDto> {

    @Getter
    @Setter
    private UUID id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private UUID implementedAlgorithm;

    @Getter
    @Setter
    private String language;

    @Getter
    @Setter
    private String selectionRule;

    @Getter
    @Setter
    private String sdk;

    @Getter
    @Setter
    private URL fileLocation;

    @Setter
    private ParameterListDto inputParameters;

    @Setter
    private ParameterListDto outputParameters;

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

        public static ImplementationDto convert(final Implementation object) {
            final ImplementationDto dto = new ImplementationDto();
            dto.setId(object.getId());
            dto.setName(object.getName());
            dto.setImplementedAlgorithm(object.getImplementedAlgorithm());
            dto.setSelectionRule(object.getSelectionRule());
            dto.setFileLocation(object.getFileLocation());
            dto.setSdk(object.getSdk().getName());
            dto.setLanguage(object.getLanguage());

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

        public static Implementation convert(final ImplementationDto object, final Sdk sdk) {
            Implementation implementation = new Implementation();
            implementation.setName(object.getName());
            implementation.setImplementedAlgorithm(object.getImplementedAlgorithm());
            implementation.setSelectionRule(object.getSelectionRule());
            implementation.setFileLocation(object.getFileLocation());
            implementation.setSdk(sdk);
            implementation.setLanguage(object.getLanguage());
            implementation.setInputParameters(object.getInputParameters().getParameters().stream()
                    .map(ParameterDto.Converter::convert)
                    .collect(Collectors.toList()));
            implementation.setOutputParameters(object.getOutputParameters().getParameters().stream()
                    .map(ParameterDto.Converter::convert)
                    .collect(Collectors.toList()));
            return implementation;
        }
    }
}
