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
import javax.transaction.Transactional;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionJobRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuSelectionJobDto;
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

    private final QpuSelectionJobRepository qpuSelectionJobRepository;

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve a single QPU selection job")
    @GetMapping("/" + Constants.JOBS + "/{resId}")
    @Transactional
    public HttpEntity<QpuSelectionJobDto> getQpuSelectionJob(@PathVariable UUID resId) {
        LOG.debug("Get to retrieve QPU selection job with id: {}.", resId);

        Optional<QpuSelectionJob> result = qpuSelectionJobRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve QPU selection result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createJobDto(result.get()), HttpStatus.OK);
    }

    private QpuSelectionJobDto createJobDto(QpuSelectionJob job) {
        QpuSelectionJobDto dto = QpuSelectionJobDto.Converter.convert(job);
        dto.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJob(job.getId())).withSelfRel());
        return dto;
    }
}
