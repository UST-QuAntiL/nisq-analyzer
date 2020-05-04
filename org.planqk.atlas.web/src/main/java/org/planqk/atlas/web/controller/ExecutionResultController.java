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

package org.planqk.atlas.web.controller;

import java.util.Optional;

import org.planqk.atlas.core.model.ExecutionResult;
import org.planqk.atlas.core.model.Implementation;
import org.planqk.atlas.core.services.ExecutionResultService;
import org.planqk.atlas.core.services.ImplementationService;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.dtos.entities.ExecutionResultDto;
import org.planqk.atlas.web.dtos.entities.ExecutionResultListDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to retrieve the results of quantum algorithm implementation executions.
 */
@RestController
@RequestMapping("/" + Constants.ALGORITHMS + "/{algoId}/" + Constants.IMPLEMENTATIONS + "/{implId}/" + Constants.RESULTS)
public class ExecutionResultController {

    private final static Logger LOG = LoggerFactory.getLogger(ExecutionResultController.class);

    private final ImplementationService implementationService;
    private final ExecutionResultService executionResultService;

    public ExecutionResultController(ImplementationService implementationService, ExecutionResultService executionResultService) {
        this.implementationService = implementationService;
        this.executionResultService = executionResultService;
    }

    @GetMapping("/")
    public HttpEntity<ExecutionResultListDto> getExecutionResults(@PathVariable Long algoId, @PathVariable Long implId) {
        LOG.debug("Get to retrieve all execution results for impl with id: {}.", implId);

        Optional<Implementation> implementationOptional = implementationService.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} form the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ExecutionResultListDto dtoList = new ExecutionResultListDto();
        for (ExecutionResult executionResult : executionResultService.findByImplementation(implementationOptional.get())) {
            dtoList.add(createExecutionResultDto(executionResult));
            dtoList.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResult(algoId, implId, executionResult.getId()))
                    .withRel(executionResult.getId().toString()));
        }

        dtoList.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResults(algoId, implId)).withSelfRel());
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @GetMapping("/{resultId}")
    public HttpEntity<ExecutionResultDto> getExecutionResult(@PathVariable Long algoId, @PathVariable Long implId, @PathVariable Long resultId) {
        LOG.debug("Get to retrieve execution result with id: {}.", resultId);

        Optional<ExecutionResult> executionResultOptional = executionResultService.findById(resultId);
        if (!executionResultOptional.isPresent()) {
            LOG.error("Unable to retrieve execution result with id {} form the repository.", resultId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createExecutionResultDto(executionResultOptional.get()), HttpStatus.CREATED);
    }

    /**
     * Create a DTO object for a given {@link ExecutionResult} with the contained data and the links to related
     * objects.
     *
     * @param executionResult the {@link ExecutionResult} to create the DTO for
     * @return the created DTO
     */
    private ExecutionResultDto createExecutionResultDto(ExecutionResult executionResult) {
        ExecutionResultDto dto = ExecutionResultDto.Converter.convert(executionResult);
        dto.add(linkTo(methodOn(ExecutionResultController.class)
                .getExecutionResult(executionResult.getExecutedImplementation().getImplementedAlgorithm().getId(), executionResult.getExecutedImplementation().getId(), executionResult.getId()))
                .withSelfRel());
        dto.add(linkTo(methodOn(ImplementationController.class)
                .getImplementation(executionResult.getExecutedImplementation().getImplementedAlgorithm().getId(), executionResult.getExecutedImplementation().getId()))
                .withRel(Constants.EXECUTED_ALGORITHM_LINK));
        dto.add(linkTo(methodOn(QpuController.class)
                .getQpu(executionResult.getExecutingQpu().getProvider().getId(), executionResult.getExecutingQpu().getId()))
                .withRel(Constants.USED_QPU_LINK));
        return dto;
    }
}
