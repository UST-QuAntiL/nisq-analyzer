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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.control.NisqAnalyzerControlService;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionJobRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ExecutionResultDto;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private final ExecutionResultRepository executionResultRepository;

    private final NisqAnalyzerControlService controlService;

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
        description = "Retrieve all QPU selection results")
    @GetMapping("/")
    @Transactional
    public HttpEntity<QpuSelectionResultListDto> getQpuSelectionResults(@RequestParam(value = "userId", required = false) String userId) {
        QpuSelectionResultListDto model = new QpuSelectionResultListDto();
        model.add(qpuSelectionResultRepository.findAllByUserId(userId).stream().map(this::createDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionResults(userId)).withSelfRel().expand());
        model.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJobs(userId)).withRel(Constants.JOBS).expand());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
        description = "Retrieve a single QPU selection result")
    @GetMapping("/{resId}")
    public HttpEntity<QpuSelectionResultDto> getQpuSelectionResult(@PathVariable UUID resId,
                                                                   @RequestParam(value = "userId", required = false) String userId) {
        LOG.debug("Get to retrieve QPU selection result with id: {}.", resId);

        Optional<QpuSelectionResult> result = qpuSelectionResultRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve QPU selection result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if ((result.get().getUserId() != null && result.get().getUserId().equals(userId))
            || (result.get().getUserId() == null && userId == null)) {
            return new ResponseEntity<>(createDto(result.get()), HttpStatus.OK);
        } else {
            LOG.error("Unable to retrieve QPU selection result with id {} for user {}.", resId, userId);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
        description = "Retrieve all QPU selection jobs")
    @GetMapping("/" + Constants.JOBS)
    @Transactional
    public HttpEntity<QpuSelectionJobListDto> getQpuSelectionJobs(
        @RequestParam(value = "userId", required = false) String userId) {
        QpuSelectionJobListDto model = new QpuSelectionJobListDto();
        model.add(qpuSelectionJobRepository.findAllByUserId(userId).stream().map(this::createJobDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJobs(userId)).withSelfRel().expand());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
        description = "Retrieve a single QPU selection job")
    @GetMapping("/" + Constants.JOBS + "/{resId}")
    @Transactional
    public HttpEntity<QpuSelectionJobDto> getQpuSelectionJob(@PathVariable UUID resId,
                                                             @RequestParam(value = "userId", required = false) String userId) {
        LOG.debug("Get to retrieve QPU selection job with id: {}.", resId);

        Optional<QpuSelectionJob> result = qpuSelectionJobRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve QPU selection job with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if ((result.get().getUserId() != null && result.get().getUserId().equals(userId))
            || (result.get().getUserId() == null && userId == null)) {
            return new ResponseEntity<>(createJobDto(result.get()), HttpStatus.OK);
        } else {
            LOG.error("Unable to retrieve QPU selection result with id {} for user {}.", resId, userId);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @Operation(responses = {@ApiResponse(responseCode = "202"), @ApiResponse(responseCode = "404", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Execute a compilation result")
    @PostMapping("/{resId}/" + Constants.EXECUTION)
    public HttpEntity<ExecutionResultDto> executeQpuSelectionResult(@PathVariable UUID resId, @RequestParam Map<String, Map<String, String>> tokens) {
        LOG.debug("Post to execute qpu-selection-result with id: {}", resId);

        Optional<QpuSelectionResult> result = qpuSelectionResultRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve qpu-selection-result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // get stored token for the execution
        QpuSelectionResult qpuSelectionResult = result.get();

        // check if target machine is of Rigetti or IBMQ, consider accordingly qvm simulator or ibmq_qasm_simulator
        String simulator;
        if (Objects.equals(qpuSelectionResult.getProvider(), Constants.RIGETTI)) {
            simulator = "qvm";
        } else {
            simulator = "qasm_simulator";
        }

        // if result to be executed is not a simulator, then check if a simulator result was already executed otherwise execute simulator first
        // required for histogram intersection
        if (!qpuSelectionResult.getQpu().contains(simulator)) {
            // get all qpuSelectionResults of same qpuSelectionJob
            List<QpuSelectionResult> jobResults =
                qpuSelectionResultRepository.findAll().stream()
                    .filter(qResult -> qResult.getQpuSelectionJobId().equals(qpuSelectionResult.getQpuSelectionJobId()))
                    .collect(Collectors.toList());
            // get qpuSelectionResult of simulator if available
            QpuSelectionResult simulatorQpuSelectionResult =
                jobResults.stream().filter(jobResult -> jobResult.getQpu().contains(simulator)).findFirst().orElse(null);
            if (Objects.nonNull(simulatorQpuSelectionResult)) {
                //check if qpu-selection result of simulator was already executed otherwise execute on simulator first
                List<ExecutionResult> simulatorExecutionResults = executionResultRepository.findByQpuSelectionResult(simulatorQpuSelectionResult);
                if (simulatorExecutionResults.size() == 0) {

                    Map<String, ParameterValue> simulatorParams = new HashMap<>();
                    simulatorParams.put(Constants.TOKEN_PARAMETER, new ParameterValue(DataType.Unknown, tokens.get("ibmq").get("ibmq")));

                    ExecutionResult simulatorExecutionResult =
                        controlService.executeCompiledQpuSelectionCircuit(simulatorQpuSelectionResult, simulatorParams);
                    ExecutionResultDto simulatorDto = ExecutionResultDto.Converter.convert(simulatorExecutionResult);
                    simulatorDto.add(
                        linkTo(methodOn(ExecutionResultController.class).getExecutionResult(simulatorExecutionResult.getId())).withSelfRel());

                    LOG.debug("Qpu-selection-result {} of {} has not yet been executed and will be executed now", simulatorQpuSelectionResult.getId(),
                        simulator);
                } else {
                    LOG.debug("Qpu-selection-result {} for simulator already executed.", qpuSelectionResult.getId());
                }
            } else {
                LOG.debug("No qpu-selection-result for simulator found in related job {} of qpu-selection-result {}.",
                    qpuSelectionResult.getQpuSelectionJobId(),
                    qpuSelectionResult.getId());
            }
        }

        Map<String, ParameterValue> params = new HashMap<>();
        if (qpuSelectionResult.getProvider().equalsIgnoreCase("ibmq")) {
            params.put(Constants.TOKEN_PARAMETER, new ParameterValue(DataType.Unknown, tokens.get("ibmq").get("ibmq")));
        } else if (qpuSelectionResult.getProvider().equalsIgnoreCase("ionq")) {
            params.put(Constants.AWS_ACCESS_TOKEN_PARAMETER, new ParameterValue(DataType.Unknown, tokens.get("ionq").get("awsAccessKey")));
            params.put(Constants.AWS_ACCESS_SECRET_PARAMETER, new ParameterValue(DataType.Unknown, tokens.get("ionq").get("awsSecretKey")));
        }

        ExecutionResult executionResult = controlService.executeCompiledQpuSelectionCircuit(qpuSelectionResult, params);
        ExecutionResultDto dto = ExecutionResultDto.Converter.convert(executionResult);
        dto.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResult(executionResult.getId())).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.ACCEPTED);
    }

    private QpuSelectionResultDto createDto(QpuSelectionResult result) {
        QpuSelectionResultDto dto = QpuSelectionResultDto.Converter.convert(result);
        dto.add(
            linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionResult(result.getId(), result.getUserId())).withSelfRel().expand());
        //dto.add(linkTo(methodOn(QpuSelectionResultController.class).executeQpuSelectionResult(result.getId(), null)).withRel(Constants.EXECUTION));
        for (ExecutionResult executionResult : executionResultRepository.findByQpuSelectionResult(result)) {
            dto.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResult(executionResult.getId()))
                .withRel(Constants.EXECUTION + "-" + executionResult.getId()));
        }
        return dto;
    }

    private QpuSelectionJobDto createJobDto(QpuSelectionJob job) {
        QpuSelectionJobDto dto = QpuSelectionJobDto.Converter.convert(job);
        dto.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJob(job.getId(), job.getUserId())).withSelfRel().expand());
        return dto;
    }
}
