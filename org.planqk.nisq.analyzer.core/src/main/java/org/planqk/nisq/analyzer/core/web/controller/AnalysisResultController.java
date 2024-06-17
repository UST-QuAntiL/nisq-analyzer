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

package org.planqk.nisq.analyzer.core.web.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.AnalysisResultRepository;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisJobDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisJobListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Tag(name = "analysis-result")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.ANALYSIS_RESULTS)
public class AnalysisResultController {
    private final static Logger LOG = LoggerFactory.getLogger(AnalysisResultController.class);

    private final AnalysisResultRepository analysisResultRepository;

    private final ExecutionResultRepository executionResultRepository;

    private final AnalysisJobRepository analysisJobRepository;

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve all analysis results for an " +
        "algorithm")
    @Parameter(in = ParameterIn.QUERY, description = "Sorting criteria in the format: property(,asc|desc). " +
        "Default sort order is ascending. " +
        "Multiple sort criteria are supported.", name = "sort", content = @Content(array = @ArraySchema(schema =
    @Schema(type = "string"))))
    @GetMapping("/algorithm/{algoId}")
    public HttpEntity<AnalysisResultListDto> getAnalysisResults(@PathVariable UUID algoId,
                                                                @Parameter(hidden = true) Sort sort) {
        LOG.debug("Get to retrieve all analysis results for algo with id: {}.", algoId);
        AnalysisResultListDto model = new AnalysisResultListDto();
        model.add(analysisResultRepository.findByImplementedAlgorithm(algoId, sort).stream()
            .map(this::createAnalysisResultDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(AnalysisResultController.class).getAnalysisResults(algoId, sort)).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve all compiler analysis jobs")
    @GetMapping("/" + Constants.JOBS)
    @Transactional
    public HttpEntity<AnalysisJobListDto> getAnalysisJobs() {
        AnalysisJobListDto model = new AnalysisJobListDto();
        model.add(
            analysisJobRepository.findAll().stream().map(this::createAnalysisJobDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisJobs()).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve a single analysis result")
    @GetMapping("/{resId}")
    public HttpEntity<AnalysisResultDto> getAnalysisResult(@PathVariable UUID resId) {
        LOG.debug("Get to retrieve analysis result with id: {}.", resId);

        Optional<AnalysisResult> result = analysisResultRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve analysis result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createAnalysisResultDto(result.get()), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve all analysis jobs for an " +
        "algorithm")
    @Parameter(in = ParameterIn.QUERY, description = "Sorting criteria in the format: property(,asc|desc). " +
        "Default sort order is ascending. " +
        "Multiple sort criteria are supported.", name = "sort", content = @Content(array = @ArraySchema(schema =
    @Schema(type = "string"))))
    @GetMapping("/" + Constants.JOBS + "/algorithm/{algoId}")
    public HttpEntity<AnalysisJobListDto> getAnalysisJobsOfAlgorithm(@PathVariable UUID algoId,
                                                                     @Parameter(hidden = true) Sort sort) {
        LOG.debug("Get to retrieve all analysis jobs for algo with id: {}.", algoId);
        AnalysisJobListDto model = new AnalysisJobListDto();
        model.add(
            analysisJobRepository.findByImplementedAlgorithm(algoId, sort).stream().map(this::createAnalysisJobDto)
                .collect(Collectors.toList()));
        model.add(
            linkTo(methodOn(AnalysisResultController.class).getAnalysisJobsOfAlgorithm(algoId, sort)).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve a single analysis job result")
    @GetMapping("/" + Constants.JOBS + "/{resId}")
    @Transactional
    public HttpEntity<AnalysisJobDto> getAnalysisJob(@PathVariable UUID resId) {
        LOG.debug("Get to retrieve analysis job with id: {}.", resId);

        Optional<AnalysisJob> result = analysisJobRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve analysis job result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createAnalysisJobDto(result.get()), HttpStatus.OK);
    }

    private AnalysisResultDto createAnalysisResultDto(AnalysisResult result) {
        AnalysisResultDto dto = AnalysisResultDto.Converter.convert(result);
        dto.add(linkTo(methodOn(AnalysisResultController.class).getAnalysisResult(result.getId())).withSelfRel());
        dto.add(linkTo(
            methodOn(ImplementationController.class).getImplementation(result.getImplementation().getId())).withRel(
            Constants.EXECUTED_ALGORITHM_LINK));
        dto.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJob(result.getQpuSelectionJobId(),
            null)).withSelfRel());
        for (ExecutionResult executionResult : executionResultRepository.findByAnalysisResult(result)) {
            dto.add(
                linkTo(methodOn(ExecutionResultController.class).getExecutionResult(executionResult.getId())).withRel(
                    Constants.EXECUTION + "-" + executionResult.getId()));
        }
        return dto;
    }

    private AnalysisJobDto createAnalysisJobDto(AnalysisJob job) {
        AnalysisJobDto dto = AnalysisJobDto.Converter.convert(job);
        dto.add(linkTo(methodOn(AnalysisResultController.class).getAnalysisJob(job.getId())).withSelfRel());
        return dto;
    }
}
