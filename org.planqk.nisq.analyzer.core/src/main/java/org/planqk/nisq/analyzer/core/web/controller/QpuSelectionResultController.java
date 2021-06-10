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
import javax.transaction.Transactional;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionJobRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuSelectionJobDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuSelectionJobListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuSelectionResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuSelectionResultListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Tag(name = "qpu-selection-result")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.QPU_SELECTION_RESULTS)
public class QpuSelectionResultController {

    private final static Logger LOG = LoggerFactory.getLogger(QpuSelectionResultController.class);

    private final QpuSelectionResultRepository qpuSelectionResultRepository;

    private final QpuSelectionJobRepository qpuSelectionJobRepository;

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve all QPU selection results")
    @GetMapping("/")
    // TODO: add time attribute
    public HttpEntity<QpuSelectionResultListDto> getQpuSelectionResults() {
        QpuSelectionResultListDto model = new QpuSelectionResultListDto();
        model.add(qpuSelectionResultRepository.findAll().stream().map(this::createDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionResults()).withSelfRel());
        model.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJobs()).withRel(Constants.JOBS));
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve a single QPU selection result")
    @GetMapping("/{resId}")
    public HttpEntity<QpuSelectionResultDto> getQpuSelectionResult(@PathVariable UUID resId) {
        LOG.debug("Get to retrieve QPU selection result with id: {}.", resId);

        Optional<QpuSelectionResult> result = qpuSelectionResultRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve QPU selection result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createDto(result.get()), HttpStatus.OK);
    }

    // TODO: /{resId}/execute, analog to compiler-selection-results/.../execute

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve all QPU selection jobs")
    @GetMapping("/" + Constants.JOBS)
    @Transactional
    public HttpEntity<QpuSelectionJobListDto> getQpuSelectionJobs() {
        QpuSelectionJobListDto model = new QpuSelectionJobListDto();
        model.add(qpuSelectionJobRepository.findAll().stream().map(this::createJobDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJobs()).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve a single QPU selection job")
    @GetMapping("/" + Constants.JOBS + "/{resId}")
    @Transactional
    // TODO: maybe add circuitName as attribute
    // TODO: add time attribute
    public HttpEntity<QpuSelectionJobDto> getQpuSelectionJob(@PathVariable UUID resId) {
        LOG.debug("Get to retrieve QPU selection job with id: {}.", resId);

        Optional<QpuSelectionJob> result = qpuSelectionJobRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve QPU selection result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createJobDto(result.get()), HttpStatus.OK);
    }

    private QpuSelectionResultDto createDto(QpuSelectionResult result) {
        QpuSelectionResultDto dto = QpuSelectionResultDto.Converter.convert(result);
        dto.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionResult(result.getId())).withSelfRel());
        return dto;
    }

    private QpuSelectionJobDto createJobDto(QpuSelectionJob job) {
        QpuSelectionJobDto dto = QpuSelectionJobDto.Converter.convert(job);
        dto.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJob(job.getId())).withSelfRel());
        return dto;
    }
}
