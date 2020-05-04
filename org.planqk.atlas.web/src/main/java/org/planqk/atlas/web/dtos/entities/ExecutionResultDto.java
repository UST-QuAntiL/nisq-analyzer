/********************************************************************************
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

import java.util.Map;

import org.planqk.atlas.core.model.ExecutionResult;
import org.planqk.atlas.core.model.ExecutionResultStatus;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

/**
 * Data transfer object for ExecutionResults ({@link org.planqk.atlas.core.model.ExecutionResult}).
 */
public class ExecutionResultDto extends RepresentationModel<ExecutionResultDto> {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private ExecutionResultStatus status;

    @Getter
    @Setter
    private String statusCode;

    @Getter
    @Setter
    private String result;

    @Getter
    @Setter
    private Map<String, String> inputParameters;

    public static final class Converter {

        public static ExecutionResultDto convert(final ExecutionResult object) {
            final ExecutionResultDto dto = new ExecutionResultDto();
            dto.setId(object.getId());
            dto.setStatus(object.getStatus());
            dto.setStatusCode(object.getStatusCode());
            dto.setResult(object.getResult());
            dto.setInputParameters(object.getInputParameters());
            return dto;
        }
    }
}
