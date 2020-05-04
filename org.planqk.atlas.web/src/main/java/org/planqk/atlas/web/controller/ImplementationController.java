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

package org.planqk.atlas.web.controller;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.planqk.atlas.core.model.Algorithm;
import org.planqk.atlas.core.model.ExecutionResult;
import org.planqk.atlas.core.model.Implementation;
import org.planqk.atlas.core.model.Qpu;
import org.planqk.atlas.core.model.Sdk;
import org.planqk.atlas.core.services.AlgorithmService;
import org.planqk.atlas.core.services.ImplementationService;
import org.planqk.atlas.core.services.QpuService;
import org.planqk.atlas.core.services.SdkService;
import org.planqk.atlas.nisq.analyzer.control.NisqAnalyzerControlService;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.dtos.entities.ExecutionResultDto;
import org.planqk.atlas.web.dtos.entities.ImplementationDto;
import org.planqk.atlas.web.dtos.entities.ImplementationListDto;
import org.planqk.atlas.web.dtos.entities.ParameterDto;
import org.planqk.atlas.web.dtos.entities.ParameterListDto;
import org.planqk.atlas.web.dtos.entities.TagListDto;
import org.planqk.atlas.web.dtos.requests.ExecutionRequest;
import org.planqk.atlas.web.utils.RestUtils;

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
import org.springframework.web.bind.annotation.RestController;

import static org.planqk.atlas.web.Constants.ALGORITHM_LINK;
import static org.planqk.atlas.web.utils.RestUtils.parameterConsistent;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to access and manipulate implementations of quantum algorithms.
 */
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.ALGORITHMS + "/{algoId}/" + Constants.IMPLEMENTATIONS)
public class ImplementationController {

    final private static Logger LOG = LoggerFactory.getLogger(ImplementationController.class);
    private final NisqAnalyzerControlService controlService;
    private final ImplementationService implementationService;
    private final AlgorithmService algorithmService;
    private final QpuService qpuService;
    private final SdkService sdkService;

    public ImplementationController(ImplementationService implementationService,
                                    AlgorithmService algorithmService,
                                    QpuService qpuService,
                                    SdkService sdkService,
                                    NisqAnalyzerControlService controlService) {
        this.implementationService = implementationService;
        this.algorithmService = algorithmService;
        this.qpuService = qpuService;
        this.sdkService = sdkService;
        this.controlService = controlService;
    }

    /**
     * Create a DTO object for a given {@link Implementation} with the contained data and the links to related objects.
     *
     * @param implementation the {@link Implementation} to create the DTO for
     * @return the created DTO
     */
    public static ImplementationDto createImplementationDto(Implementation implementation) {
        Long algoId = implementation.getImplementedAlgorithm().getId();
        ImplementationDto dto = ImplementationDto.Converter.convert(implementation);
        dto.add(linkTo(methodOn(ImplementationController.class).getImplementation(algoId, implementation.getId())).withSelfRel());
        dto.add(linkTo(methodOn(AlgorithmController.class).getAlgorithm(algoId)).withRel(Constants.ALGORITHM_LINK));
        dto.add(linkTo(methodOn(ImplementationController.class).getTags(algoId, implementation.getId())).withRel(Constants.TAGS));

        Sdk usedSdk = implementation.getSdk();
        if (Objects.nonNull(usedSdk)) {
            dto.add(linkTo(methodOn(SdkController.class).getSdk(usedSdk.getId())).withRel(Constants.USED_SDK));
        }

        return dto;
    }

    @GetMapping("/")
    public HttpEntity<ImplementationListDto> getImplementations(@PathVariable Long algoId) {
        LOG.debug("Get to retrieve all implementations received.");
        ImplementationListDto dtoList = new ImplementationListDto();

        // add all available implementations to the response
        for (Implementation impl : implementationService.findAll(RestUtils.getAllPageable())) {
            if (impl.getImplementedAlgorithm().getId().equals(algoId)) {
                dtoList.add(createImplementationDto(impl));
                dtoList.add(linkTo(methodOn(ImplementationController.class).getImplementation(algoId, impl.getId()))
                        .withRel(impl.getId().toString()));
            }
        }

        // add links and status code
        dtoList.add(linkTo(methodOn(ImplementationController.class).getImplementations(algoId)).withSelfRel());
        dtoList.add(linkTo(methodOn(AlgorithmController.class).getAlgorithm(algoId)).withRel(ALGORITHM_LINK));
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @GetMapping("/{implId}")
    public HttpEntity<ImplementationDto> getImplementation(@PathVariable Long algoId, @PathVariable Long implId) {
        LOG.debug("Get to retrieve implementation with id: {}.", implId);

        Optional<Implementation> implementationOptional = implementationService.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} form the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createImplementationDto(implementationOptional.get()), HttpStatus.OK);
    }

    @PostMapping("/")
    public HttpEntity<ImplementationDto> createImplementation(@PathVariable Long algoId, @RequestBody ImplementationDto impl) {
        LOG.debug("Post to create new implementation received.");

        Optional<Algorithm> algorithmOptional = algorithmService.findById(algoId);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} from the repository.", algoId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // check consistency of the implementation object
        if (Objects.isNull(impl.getName())
                || Objects.isNull(impl.getProgrammingLanguage()) || Objects.isNull(impl.getSelectionRule())
                || Objects.isNull(impl.getSdk()) || Objects.isNull(impl.getFileLocation())) {
            LOG.error("Received invalid implementation object for post request: {}", impl.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // retrieve referenced Sdk and abort if not present
        Optional<Sdk> sdkOptional = sdkService.findByName(impl.getSdk());
        if (!sdkOptional.isPresent()) {
            LOG.error("Unable to retrieve Sdk with name {} from the repository.", impl.getSdk());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // check consistency of passed parameters
        if (!parameterConsistent(impl.getInputParameters().getParameters(), impl.getOutputParameters().getParameters())) {
            LOG.error("Received invalid parameter dto for post request.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        LOG.debug("Received post request contains consistent data. Storing entity...");

        // store and return implementation
        Implementation implementation =
                implementationService.save(ImplementationDto.Converter.convert(impl, sdkOptional.get(), algorithmOptional.get()));
        return new ResponseEntity<>(createImplementationDto(algoId, implementation), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/" + Constants.INPUT_PARAMS)
    public HttpEntity<ParameterListDto> getInputParameters(@PathVariable Long algoId, @PathVariable Long id) {
        LOG.debug("Get to retrieve input parameters for implementation with id: {}.", id);

        Optional<Implementation> implementationOptional = implementationService.findById(id);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // convert all input parameters to corresponding dtos
        ParameterListDto parameterListDto = new ParameterListDto();
        parameterListDto.add(implementationOptional.get().getInputParameters().stream()
                .map(ParameterDto.Converter::convert)
                .collect(Collectors.toList()));

        parameterListDto.add(linkTo(methodOn(AlgorithmController.class).getInputParameters(id)).withSelfRel());
        return new ResponseEntity<>(parameterListDto, HttpStatus.OK);
    }

    @GetMapping("/{id}/" + Constants.OUTPUT_PARAMS)
    public HttpEntity<ParameterListDto> getOutputParameters(@PathVariable Long algoId, @PathVariable Long id) {
        LOG.debug("Get to retrieve output parameters for implementation with id: {}.", id);

        Optional<Implementation> implementationOptional = implementationService.findById(id);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // convert all output parameters to corresponding dtos
        ParameterListDto parameterListDto = new ParameterListDto();
        parameterListDto.add(implementationOptional.get().getOutputParameters().stream()
                .map(ParameterDto.Converter::convert)
                .collect(Collectors.toList()));

        parameterListDto.add(linkTo(methodOn(AlgorithmController.class).getOutputParameters(id)).withSelfRel());
        return new ResponseEntity<>(parameterListDto, HttpStatus.OK);
    }

    @PostMapping("/{id}/" + Constants.INPUT_PARAMS)
    public HttpEntity<ParameterDto> addInputParameter(@PathVariable Long id, @RequestBody ParameterDto parameterDto) {
        LOG.debug("Post to add input parameter on implementation with id: {}.", id);
        Optional<Implementation> implementationOptional = implementationService.findById(id);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (Objects.isNull(parameterDto.getName())) {
            LOG.error("Received invalid parameter object for post request: {}", parameterDto.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Implementation implementation = implementationOptional.get();
        implementation.getInputParameters().add(ParameterDto.Converter.convert(parameterDto));
        implementationService.save(implementation);
        return new ResponseEntity<>(parameterDto, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/" + Constants.OUTPUT_PARAMS)
    public HttpEntity<ParameterDto> addOutputParameter(@PathVariable Long id, @RequestBody ParameterDto parameterDto) {
        LOG.debug("Post to add output parameter on implementation with id: {}.", id);
        Optional<Implementation> implementationOptional = implementationService.findById(id);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (Objects.isNull(parameterDto.getName())) {
            LOG.error("Received invalid parameter object for post request: {}", parameterDto.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Implementation implementation = implementationOptional.get();
        implementation.getOutputParameters().add(ParameterDto.Converter.convert(parameterDto));
        implementationService.save(implementation);
        return new ResponseEntity<>(parameterDto, HttpStatus.CREATED);
    }

    @PostMapping("/{implId}/" + Constants.EXECUTION)
    public HttpEntity<ExecutionResultDto> executeImplementation(@PathVariable Long algoId, @PathVariable Long implId,
                                                                @RequestBody ExecutionRequest executionRequest) {
        LOG.debug("Post to execute implementation with Id: {}", implId);

        Optional<Implementation> implementationOptional = implementationService.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Optional<Qpu> qpuOptional = qpuService.findById(executionRequest.getQpuId());
        if (!qpuOptional.isPresent()) {
            LOG.error("Unable to retrieve qpu with id {} form the repository.", executionRequest.getQpuId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            ExecutionResult result = controlService.executeQuantumAlgorithmImplementation(implementationOptional.get(), qpuOptional.get(), executionRequest.getParameters());
            ExecutionResultDto dto = ExecutionResultDto.Converter.convert(result);
            dto.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResult(algoId, implId, result.getId())).withSelfRel());
            return new ResponseEntity<>(dto, HttpStatus.ACCEPTED);
        } catch (RuntimeException e) {
            LOG.error("Error while executing implementation: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{implId}/" + Constants.TAGS)
    public HttpEntity<TagListDto> getTags(@PathVariable Long algoId, @PathVariable Long implId) {
        Optional<Implementation> implementationOptional = implementationService.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to find implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        TagListDto tagListDto = TagController.createTagDtoList(implementationOptional.get().getTags().stream());
        tagListDto.add(linkTo(methodOn(ImplementationController.class).getTags(algoId, implId)).withSelfRel());
        return new ResponseEntity<>(tagListDto, HttpStatus.OK);
    }

    /**
     * Create a DTO object for a given {@link Implementation} with the contained data and the links to related objects.
     *
     * @param algoId         the Id of the Algorithm this Implementation belongs to
     * @param implementation the {@link Implementation} to create the DTO for
     * @return the created DTO
     */
    private ImplementationDto createImplementationDto(Long algoId, Implementation implementation) {
        ImplementationDto dto = ImplementationDto.Converter.convert(implementation);
        dto.add(linkTo(methodOn(ImplementationController.class).getImplementation(algoId, implementation.getId())).withSelfRel());
        dto.add(linkTo(methodOn(AlgorithmController.class).getAlgorithm(algoId)).withRel(Constants.ALGORITHM_LINK));
        dto.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResults(algoId, implementation.getId())).withRel(Constants.RESULTS_LINK));

        Sdk usedSdk = implementation.getSdk();
        if (Objects.nonNull(usedSdk)) {
            dto.add(linkTo(methodOn(SdkController.class).getSdk(usedSdk.getId())).withRel(Constants.USED_SDK));
        }

        return dto;
    }
}
