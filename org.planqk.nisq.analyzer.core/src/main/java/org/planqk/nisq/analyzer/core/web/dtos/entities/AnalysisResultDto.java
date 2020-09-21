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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.springframework.hateoas.RepresentationModel;

@EqualsAndHashCode(callSuper = false)
@Data
public class AnalysisResultDto extends RepresentationModel<AnalysisResultDto> {

    QpuDto qpu;

    ImplementationDto implementation;

    boolean estimate;

    int analysedDepth;

    int analysedWidth;

    public static final class Converter {

        public static AnalysisResultDto convert(final AnalysisResult object) {
            AnalysisResultDto dto = new AnalysisResultDto();
            dto.setQpu(QpuDto.Converter.convert(object.getQpu()));
            dto.setImplementation(ImplementationDto.Converter.convert(object.getImplementation()));
            dto.setAnalysedDepth(object.getAnalysedDepth());
            dto.setAnalysedWidth(object.getAnalysedWidth());
            return dto;
        }
    }
}
