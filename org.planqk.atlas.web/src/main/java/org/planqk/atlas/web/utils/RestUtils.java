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

package org.planqk.atlas.web.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.planqk.atlas.core.model.Parameter;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.controller.AlgorithmController;
import org.planqk.atlas.web.dtos.entities.ParameterDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Utility class for the REST API functionality
 */
public class RestUtils {

    final private static Logger LOG = LoggerFactory.getLogger(AlgorithmController.class);

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

    /**
     * Check if all required parameters are contained in the provided parameters
     *
     * @param requiredParameters the set of required parameters
     * @param providedParameters the map with the provided parameters
     * @return <code>true</code> if all required parameters are contained in the provided parameters, <code>false</code>
     * otherwise
     */
    public static boolean parametersAvailable(Set<Parameter> requiredParameters, Map<String, String> providedParameters) {
        LOG.debug("Checking if {} required parameters are available in the input map with {} provided parameters!", requiredParameters.size(), providedParameters.size());
        return requiredParameters.stream().allMatch(param -> providedParameters.containsKey(param.getName()));
    }

    /**
     * Return a (default) pageable from the provided Requestparams for an endpoint that can be used with pagination
     *
     * @param size the size of a page
     * @param page the number of the page that should be returned
     * @return construct the <code>Pageable</code> if suitable parameters are given, <code>Pageable.unpaged()</code> (no
     * Pagination) otherwise
     */
    public static Pageable getPageableFromRequestParams(Integer page, Integer size) {
        if (size != null && page != null) {
            return PageRequest.of(page, size);
        }
        if (size != null) { // default start page to 0
            return PageRequest.of(0, size);
        } // default if no pagination params are set:
        return Pageable.unpaged();
    }

    /**
     * Returns unpaged Paginationparams
     */
    public static Pageable getAllPageable() {
        return Pageable.unpaged();
    }

    /**
     * Returns default Paginationparams
     */
    public static Pageable getDefaultPageable() {
        return PageRequest.of(Constants.DEFAULT_PAGE_NUMBER, Constants.DEFAULT_PAGE_SIZE);
    }
}
