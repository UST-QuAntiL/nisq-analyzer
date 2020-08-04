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

package org.planqk.nisq.analyzer.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for the REST API functionality
 */
public class RestUtils {

    final private static Logger LOG = LoggerFactory.getLogger(RestUtils.class);

    /**
     * Check if the given lists of input and output parameters are consistent and contain the required attributes to
     * store them in the repository
     *
     * @param inputParameters  the list of input parameters
     * @param outputParameters the list of output parameters
     * @return <code>true</code> if all parameters are consistens, <code>false</code> otherwise
     */
    public static boolean parameterConsistent(List<ParameterDto> inputParameters, List<ParameterDto> outputParameters) {
        // avoid changing the potential live lists that are passed
        List<ParameterDto> parameters = new ArrayList<>();
        parameters.addAll(inputParameters);
        parameters.addAll(outputParameters);

        for (ParameterDto param : parameters) {
            if (Objects.isNull(param.getName()) || Objects.isNull(param.getType())) {
                LOG.error("Invalid parameter: {}", param.toString());
                return false;
            }
        }
        return true;
    }
}
