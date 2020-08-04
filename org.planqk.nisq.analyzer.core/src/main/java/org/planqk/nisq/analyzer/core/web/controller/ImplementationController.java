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

package org.planqk.nisq.analyzer.core.web.controller;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.control.NisqAnalyzerControlService;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.QpuRepository;
import org.planqk.nisq.analyzer.core.repository.SdkRepository;
import org.planqk.nisq.analyzer.core.utils.RestUtils;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ExecutionResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ImplementationDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ImplementationListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterListDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.ExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to access and manipulate implementations of quantum algorithms.
 */
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.IMPLEMENTATIONS)
public class ImplementationController {

    final private static Logger LOG = LoggerFactory.getLogger(ImplementationController.class);
    private final NisqAnalyzerControlService controlService;
    private final ImplementationRepository implementationRepository;
    private final QpuRepository qpuRepository;
    private final SdkRepository sdkRepository;

    public ImplementationController(ImplementationRepository implementationRepository,
                                    QpuRepository qpuRepository,
                                    SdkRepository sdkRepository,
                                    NisqAnalyzerControlService controlService) {
        this.implementationRepository = implementationRepository;
        this.qpuRepository = qpuRepository;
        this.sdkRepository = sdkRepository;
        this.controlService = controlService;
    }

    @GetMapping("/")
    public HttpEntity<ImplementationListDto> getImplementations(@RequestParam(required = false) Long algoId) {
        LOG.debug("Get to retrieve all implementations received.");
        ImplementationListDto dtoList = new ImplementationListDto();

        // add all available implementations to the response
        for (Implementation impl : implementationRepository.findAll()) {
            // skip impl if query parameter is defined and algo id does not match
            if (Objects.nonNull(algoId) && !impl.getImplementedAlgorithm().equals(algoId)) {
                continue;
            }
            dtoList.add(createImplementationDto(impl));
            dtoList.add(linkTo(methodOn(ImplementationController.class).getImplementation(impl.getId()))
                    .withRel(impl.getId().toString()));
        }

        // add links and status code
        dtoList.add(linkTo(methodOn(ImplementationController.class).getImplementations(algoId)).withSelfRel());
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @GetMapping("/{implId}")
    public HttpEntity<ImplementationDto> getImplementation(@PathVariable Long implId) {
        LOG.debug("Get to retrieve implementation with id: {}.", implId);

        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} form the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createImplementationDto(implementationOptional.get()), HttpStatus.OK);
    }

    @PostMapping("/")
    public HttpEntity<ImplementationDto> createImplementation(@RequestBody ImplementationDto impl) {
        LOG.debug("Post to create new implementation received.");

        // check consistency of the implementation object
        if (Objects.isNull(impl.getName())
                || Objects.isNull(impl.getImplementedAlgorithm()) || Objects.isNull(impl.getSelectionRule())
                || Objects.isNull(impl.getSdk()) || Objects.isNull(impl.getFileLocation())) {
            LOG.error("Received invalid implementation object for post request: {}", impl.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // retrieve referenced Sdk and abort if not present
        Optional<Sdk> sdkOptional = sdkRepository.findByName(impl.getSdk());
        if (!sdkOptional.isPresent()) {
            LOG.error("Unable to retrieve Sdk with name {} from the repository.", impl.getSdk());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // check consistency of passed parameters
        if (!RestUtils.parameterConsistent(impl.getInputParameters().getParameters(), impl.getOutputParameters().getParameters())) {
            LOG.error("Received invalid parameter dto for post request.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        LOG.debug("Received post request contains consistent data. Storing entity...");

        // store and return implementation
        Implementation implementation =
                implementationRepository.save(ImplementationDto.Converter.convert(impl, sdkOptional.get()));
        return new ResponseEntity<>(createImplementationDto(implementation), HttpStatus.CREATED);
    }

    @GetMapping("/{implId}/" + Constants.INPUT_PARAMS)
    public HttpEntity<ParameterListDto> getInputParameters(@PathVariable Long implId) {
        LOG.debug("Get to retrieve input parameters for implementation with id: {}.", implId);

        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // convert all input parameters to corresponding dtos
        ParameterListDto parameterListDto = new ParameterListDto();
        parameterListDto.add(implementationOptional.get().getInputParameters().stream()
                .map(ParameterDto.Converter::convert)
                .collect(Collectors.toList()));

        parameterListDto.add(linkTo(methodOn(ImplementationController.class).getInputParameters(implId)).withSelfRel());
        return new ResponseEntity<>(parameterListDto, HttpStatus.OK);
    }

    @GetMapping("/{implId}/" + Constants.OUTPUT_PARAMS)
    public HttpEntity<ParameterListDto> getOutputParameters(@PathVariable Long implId) {
        LOG.debug("Get to retrieve output parameters for implementation with id: {}.", implId);

        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // convert all output parameters to corresponding dtos
        ParameterListDto parameterListDto = new ParameterListDto();
        parameterListDto.add(implementationOptional.get().getOutputParameters().stream()
                .map(ParameterDto.Converter::convert)
                .collect(Collectors.toList()));

        parameterListDto.add(linkTo(methodOn(ImplementationController.class).getOutputParameters(implId)).withSelfRel());
        return new ResponseEntity<>(parameterListDto, HttpStatus.OK);
    }

    @PostMapping("/{implId}/" + Constants.INPUT_PARAMS)
    public HttpEntity<ParameterDto> addInputParameter(@PathVariable Long implId, @RequestBody ParameterDto parameterDto) {
        LOG.debug("Post to add input parameter on implementation with id: {}.", implId);
        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (Objects.isNull(parameterDto.getName())) {
            LOG.error("Received invalid parameter object for post request: {}", parameterDto.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Implementation implementation = implementationOptional.get();
        implementation.getInputParameters().add(ParameterDto.Converter.convert(parameterDto));
        implementationRepository.save(implementation);
        return new ResponseEntity<>(parameterDto, HttpStatus.CREATED);
    }

    @PostMapping("/{implId}/" + Constants.OUTPUT_PARAMS)
    public HttpEntity<ParameterDto> addOutputParameter(@PathVariable Long implId, @RequestBody ParameterDto parameterDto) {
        LOG.debug("Post to add output parameter on implementation with id: {}.", implId);
        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (Objects.isNull(parameterDto.getName())) {
            LOG.error("Received invalid parameter object for post request: {}", parameterDto.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Implementation implementation = implementationOptional.get();
        implementation.getOutputParameters().add(ParameterDto.Converter.convert(parameterDto));
        implementationRepository.save(implementation);
        return new ResponseEntity<>(parameterDto, HttpStatus.CREATED);
    }

    @PostMapping("/{implId}/" + Constants.EXECUTION)
    public HttpEntity<ExecutionResultDto> executeImplementation(@PathVariable Long implId,
                                                                @RequestBody ExecutionRequest executionRequest) {
        LOG.debug("Post to execute implementation with Id: {}", implId);

        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<Qpu> qpuOptional = qpuRepository.findById(executionRequest.getQpuId());
        if (!qpuOptional.isPresent()) {
            LOG.error("Unable to retrieve qpu with id {} form the repository.", executionRequest.getQpuId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            ExecutionResult result = controlService.executeQuantumAlgorithmImplementation(implementationOptional.get(), qpuOptional.get(), executionRequest.getParameters());
            ExecutionResultDto dto = ExecutionResultDto.Converter.convert(result);
            dto.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResult(implId, result.getId())).withSelfRel());
            return new ResponseEntity<>(dto, HttpStatus.ACCEPTED);
        } catch (RuntimeException e) {
            LOG.error("Error while executing implementation: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a DTO object for a given {@link Implementation} with the contained data and the links to related objects.
     *
     * @param implementation the {@link Implementation} to create the DTO for
     * @return the created DTO
     */
    private ImplementationDto createImplementationDto(Implementation implementation) {
        ImplementationDto dto = ImplementationDto.Converter.convert(implementation);
        dto.add(linkTo(methodOn(ImplementationController.class).getImplementation(implementation.getId())).withSelfRel());
        dto.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResults(implementation.getId())).withRel(Constants.RESULTS_LINK));

        Sdk usedSdk = implementation.getSdk();
        if (Objects.nonNull(usedSdk)) {
            dto.add(linkTo(methodOn(SdkController.class).getSdk(usedSdk.getId())).withRel(Constants.USED_SDK));
        }

        return dto;
    }
}
