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

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.control.NisqAnalyzerControlService;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologFactUpdater;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.SdkRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ExecutionResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ImplementationDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ImplementationListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterListDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.ExecutionRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to access and manipulate implementations of quantum algorithms.
 */
@Tag(name = "implementation")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.IMPLEMENTATIONS)
public class ImplementationController {

    final private static Logger LOG = LoggerFactory.getLogger(ImplementationController.class);
    private final NisqAnalyzerControlService controlService;
    private final ImplementationRepository implementationRepository;
    private final SdkRepository sdkRepository;
    private final PrologFactUpdater prologFactUpdater;

    public ImplementationController(ImplementationRepository implementationRepository,
                                    SdkRepository sdkRepository,
                                    NisqAnalyzerControlService controlService,
                                    PrologFactUpdater prologFactUpdater) {
        this.implementationRepository = implementationRepository;
        this.sdkRepository = sdkRepository;
        this.controlService = controlService;
        this.prologFactUpdater = prologFactUpdater;
    }

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

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve implementations for an algorithm")
    @GetMapping("/")
    public HttpEntity<ImplementationListDto> getImplementations(@RequestParam(required = false) UUID algoId) {
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

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve an implementation")
    @GetMapping("/{implId}")
    public HttpEntity<ImplementationDto> getImplementation(@PathVariable UUID implId) {
        LOG.debug("Get to retrieve implementation with id: {}.", implId);

        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} form the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createImplementationDto(implementationOptional.get()), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "201"), @ApiResponse(responseCode = "400", content = @Content)},
            description = "Create an implementation")
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
        if (!parameterConsistent(impl.getInputParameters().getParameters(), impl.getOutputParameters().getParameters())) {
            LOG.error("Received invalid parameter dto for post request.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        LOG.debug("Received post request contains consistent data. Storing entity...");

        // store and return implementation
        Implementation implementation =
                implementationRepository.save(ImplementationDto.Converter.convert(impl, sdkOptional.get()));
        prologFactUpdater.handleImplementationInsertion(implementation);
        return new ResponseEntity<>(createImplementationDto(implementation), HttpStatus.CREATED);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content)},
            description = "Update an implementation")
    @PutMapping("/{implId}")
    public HttpEntity<ImplementationDto> updateImplementation(@PathVariable UUID implId, @RequestBody ImplementationDto impl) {
        LOG.debug("Post to update a new implementation received.");

        // check consistency of the implementation object
        if (Objects.isNull(impl.getName())
                || Objects.isNull(impl.getImplementedAlgorithm()) || Objects.isNull(impl.getSelectionRule())
                || Objects.isNull(impl.getSdk()) || Objects.isNull(impl.getFileLocation())) {
            LOG.error("Received invalid implementation object for put request: {}", impl.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // retrieve referenced Sdk and abort if not present
        Optional<Sdk> sdkOptional = sdkRepository.findByName(impl.getSdk());
        if (!sdkOptional.isPresent()) {
            LOG.error("Unable to retrieve Sdk with name {} from the repository.", impl.getSdk());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Implementation oldImpl = implementationOptional.get();
        Implementation newImpl = ImplementationDto.Converter.convert(impl, sdkOptional.get());

        // We can't take the parameter lists from our request, as they don't contain IDs
        newImpl.setId(implId);
        newImpl.setInputParameters(oldImpl.getInputParameters());
        newImpl.setOutputParameters(oldImpl.getOutputParameters());

        prologFactUpdater.handleImplementationInsertion(newImpl);
        newImpl = implementationRepository.save(newImpl);
        return new ResponseEntity<>(createImplementationDto(newImpl), HttpStatus.CREATED);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve input parameters for an implementation")
    @GetMapping("/{implId}/" + Constants.INPUT_PARAMS)
    public HttpEntity<ParameterListDto> getInputParameters(@PathVariable UUID implId) {
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

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve output parameters for an implementation")
    @GetMapping("/{implId}/" + Constants.OUTPUT_PARAMS)
    public HttpEntity<ParameterListDto> getOutputParameters(@PathVariable UUID implId) {
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

    @Operation(responses = {@ApiResponse(responseCode = "201"), @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "400", content = @Content)}, description = "Add input parameters to an implementation")
    @PostMapping("/{implId}/" + Constants.INPUT_PARAMS)
    public HttpEntity<ParameterDto> addInputParameter(@PathVariable UUID implId, @RequestBody ParameterDto parameterDto) {
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

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Remove input parameters from an implementation")
    @DeleteMapping("/{implId}/" + Constants.INPUT_PARAMS)
    public HttpEntity<Void> deleteInputParameters(@PathVariable UUID implId, @RequestBody List<String> names) {
        LOG.debug("Delete to remove input parameter from implementation with id: {}.", implId);
        Optional<Implementation> implementationOptional = implementationRepository.findById(implId);
        if (!implementationOptional.isPresent()) {
            LOG.error("Unable to retrieve implementation with id {} from the repository.", implId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Implementation implementation = implementationOptional.get();
        implementation.getInputParameters().removeIf(parameter -> names.contains(parameter.getName()));
        implementationRepository.save(implementation);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "201"), @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "400", content = @Content)}, description = "Add output parameters to an implementation")
    @PostMapping("/{implId}/" + Constants.OUTPUT_PARAMS)
    public HttpEntity<ParameterDto> addOutputParameter(@PathVariable UUID implId, @RequestBody ParameterDto parameterDto) {
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
        dto.add(linkTo(methodOn(ImplementationController.class).getInputParameters(implementation.getId())).withRel(Constants.INPUT_PARAMS));
        dto.add(linkTo(methodOn(ImplementationController.class).getOutputParameters(implementation.getId())).withRel(Constants.OUTPUT_PARAMS));

        Sdk usedSdk = implementation.getSdk();
        if (Objects.nonNull(usedSdk)) {
            dto.add(linkTo(methodOn(SdkController.class).getSdk(usedSdk.getId())).withRel(Constants.USED_SDK));
        }

        return dto;
    }
}
