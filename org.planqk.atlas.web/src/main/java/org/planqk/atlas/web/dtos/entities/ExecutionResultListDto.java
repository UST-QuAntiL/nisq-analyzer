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

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import org.assertj.core.util.Lists;
import org.springframework.hateoas.RepresentationModel;

/**
 * Data transfer object for multiple ExecutionResults ({@link org.planqk.atlas.core.model.ExecutionResult}).
 */
public class ExecutionResultListDto extends RepresentationModel<ExecutionResultListDto> {

    @Getter
    private final List<ExecutionResultDto> executionResultDtos = Lists.newArrayList();

    public void add(final ExecutionResultDto... executionResults) {
        this.executionResultDtos.addAll(Arrays.asList(executionResults));
    }
}
