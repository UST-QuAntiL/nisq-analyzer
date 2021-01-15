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

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "analysisResult", collectionRelation = "analysisResults")
@EqualsAndHashCode(callSuper = false)
@Data
public class AnalysisResultDto extends RepresentationModel<AnalysisResultDto> {

    UUID id;

    String qpu;

    String provider;

    ImplementationDto implementation;

    int analyzedDepth;

    int analyzedWidth;

    private Map<String, String> inputParameters;

    private OffsetDateTime time;

    public static final class Converter {

        public static AnalysisResultDto convert(final AnalysisResult object) {
            AnalysisResultDto dto = new AnalysisResultDto();
            dto.setId(object.getId());
            dto.setQpu(object.getQpu());
            dto.setProvider(object.getProvider());
            dto.setImplementation(ImplementationDto.Converter.convert(object.getImplementation()));
            dto.setAnalyzedDepth(object.getAnalyzedDepth());
            dto.setAnalyzedWidth(object.getAnalyzedWidth());
            dto.setInputParameters(object.getInputParameters());
            dto.setTime(object.getTime());
            return dto;
        }
    }
}
