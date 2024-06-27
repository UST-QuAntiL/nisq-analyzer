/*******************************************************************************
 * Copyright (c) 2024 University of Stuttgart
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
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.model.AnalysisJob;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AnalysisJobDto extends AnalysisResultListDto {

    private UUID id;

    private UUID implementedAlgorithm;

    private OffsetDateTime time;

    private Map<String, String> inputParameters;

    private boolean ready;

    private UUID initialMcdaJob;

    private String initialMcdaMethod;

    public static final class Converter {

        public static AnalysisJobDto convert(final AnalysisJob object) {
            AnalysisJobDto dto = new AnalysisJobDto();
            dto.setId(object.getId());
            dto.setImplementedAlgorithm(object.getImplementedAlgorithm());
            dto.setTime(object.getTime());
            dto.setInputParameters(object.getInputParameters());
            dto.setReady(object.isReady());
            dto.setInitialMcdaJob(object.getInitialMcdaJob());
            dto.setInitialMcdaMethod(object.getInitialMcdaMethod());
            dto.add(
                object.getJobResults().stream().map(AnalysisResultDto.Converter::convert).collect(Collectors.toList()));

            return dto;
        }
    }
}
