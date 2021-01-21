/*******************************************************************************
 * Copyright (c) 2021 University of Stuttgart
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

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.CompilationResult;
import org.planqk.nisq.analyzer.core.repository.CompilerAnalysisResultRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.CompilerAnalysisResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.CompilerAnalysisResultListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ExecutionResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Tag(name = "compiler-analysis-result")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.COMPILER_RESULTS)
public class CompilerAnalysisResultController {

    private final static Logger LOG = LoggerFactory.getLogger(AnalysisResultController.class);

    private final CompilerAnalysisResultRepository compilerAnalysisResultRepository;

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve all compiler analysis results")
    @GetMapping("/")
    public HttpEntity<CompilerAnalysisResultListDto> getCompilerAnalysisResults() {
        CompilerAnalysisResultListDto model = new CompilerAnalysisResultListDto();
        model.add(compilerAnalysisResultRepository.findAll().stream().map(this::createDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisResults()).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve a single compilation result")
    @GetMapping("/{resId}")
    public HttpEntity<CompilerAnalysisResultDto> getCompilerAnalysisResult(@PathVariable UUID resId) {
        LOG.debug("Get to retrieve compilation result with id: {}.", resId);

        Optional<CompilationResult> result = compilerAnalysisResultRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve compilation result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createDto(result.get()), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "202"), @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "500", content = @Content)}, description = "Execute a compilation result")
    @PostMapping("/{resId}/" + Constants.EXECUTION)
    public HttpEntity<ExecutionResultDto> executeCompilationResult(@PathVariable UUID resId) {
        LOG.debug("Post to execute compilation result with id: {}", resId);

        Optional<CompilationResult> result = compilerAnalysisResultRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve compilation result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final CompilationResult compilationResult = result.get();
        // TODO: execute result
        return null;
    }

    private CompilerAnalysisResultDto createDto(CompilationResult result) {
        CompilerAnalysisResultDto dto = CompilerAnalysisResultDto.Converter.convert(result);
        dto.add(linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisResult(result.getId())).withSelfRel());
        dto.add(linkTo(methodOn(CompilerAnalysisResultController.class).executeCompilationResult(result.getId())).withRel(Constants.EXECUTION));
        return dto;
    }
}
