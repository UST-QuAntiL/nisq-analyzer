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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.planqk.atlas.core.model.Algorithm;
import org.planqk.atlas.core.model.Implementation;
import org.planqk.atlas.core.model.Qpu;
import org.planqk.atlas.core.model.Tag;
import org.planqk.atlas.core.services.AlgorithmService;
import org.planqk.atlas.nisq.analyzer.control.NisqAnalyzerControlService;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.dtos.entities.AlgorithmDto;
import org.planqk.atlas.web.dtos.entities.AlgorithmListDto;
import org.planqk.atlas.web.dtos.entities.ImplementationDto;
import org.planqk.atlas.web.dtos.entities.ParameterDto;
import org.planqk.atlas.web.dtos.entities.ParameterListDto;
import org.planqk.atlas.web.dtos.entities.QpuDto;
import org.planqk.atlas.web.dtos.entities.TagListDto;
import org.planqk.atlas.web.dtos.requests.ParameterKeyValueDto;
import org.planqk.atlas.web.dtos.requests.SuitableImplQpuPairsDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.planqk.atlas.web.utils.RestUtils.parameterConsistent;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to access and manipulate quantum algorithms.
 */
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.ALGORITHMS)
public class AlgorithmController {

    final private static Logger LOG = LoggerFactory.getLogger(AlgorithmController.class);

    private final NisqAnalyzerControlService nisqAnalyzerService;
    private final AlgorithmService algorithmService;

    public AlgorithmController(NisqAnalyzerControlService nisqAnalyzerService, AlgorithmService algorithmService) {
        this.nisqAnalyzerService = nisqAnalyzerService;
        this.algorithmService = algorithmService;
    }

    /**
     * Create a DTO object for a given {@link Algorithm} with the contained data and the links to related objects.
     *
     * @param algorithm the {@link Algorithm} to create the DTO for
     * @return the created DTO
     */
    public static AlgorithmDto createAlgorithmDto(Algorithm algorithm) {
        AlgorithmDto dto = AlgorithmDto.Converter.convert(algorithm);
        dto.add(linkTo(methodOn(AlgorithmController.class).getAlgorithm(algorithm.getId())).withSelfRel());
        dto.add(linkTo(methodOn(AlgorithmController.class).getInputParameters(algorithm.getId())).withRel(Constants.INPUT_PARAMS));
        dto.add(linkTo(methodOn(AlgorithmController.class).getOutputParameters(algorithm.getId())).withRel(Constants.OUTPUT_PARAMS));
        dto.add(linkTo(methodOn(AlgorithmController.class).getSelectionParams(algorithm.getId())).withRel(Constants.SELECTION_PARAMS));
        dto.add(linkTo(methodOn(AlgorithmController.class).getTags(algorithm.getId())).withRel(Constants.TAGS));
        dto.add(linkTo(methodOn(ImplementationController.class).getImplementations(algorithm.getId())).withRel(Constants.IMPLEMENTATIONS));
        return dto;
    }

    @GetMapping("/")
    public HttpEntity<AlgorithmListDto> getAlgorithms(@RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        LOG.debug("Get to retrieve all algorithms received.");
        AlgorithmListDto dtoList = new AlgorithmListDto();

        // add all available algorithms to the response
        for (Algorithm algo : algorithmService.findAll(RestUtils.getPageableFromRequestParams(page, size))) {
            dtoList.add(createAlgorithmDto(algo));
            dtoList.add(linkTo(methodOn(AlgorithmController.class).getAlgorithm(algo.getId())).withRel(algo.getId().toString()));
        }

        // add self link and status code
        dtoList.add(linkTo(methodOn(AlgorithmController.class).getAlgorithms(null, null)).withSelfRel());
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @PostMapping("/")
    public HttpEntity<AlgorithmDto> createAlgorithm(@RequestBody AlgorithmDto algo) {
        LOG.debug("Post to create new algorithm received.");

        if (Objects.isNull(algo.getName())) {
            LOG.error("Received invalid algorithm object for post request: {}", algo.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // check consistency of passed parameters
        if (!parameterConsistent(algo.getInputParameters().getParameters(), algo.getOutputParameters().getParameters())) {
            LOG.error("Received invalid parameter dto for post request.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // store and return algorithm
        Algorithm algorithm = algorithmService.save(AlgorithmDto.Converter.convert(algo));
        return new ResponseEntity<>(createAlgorithmDto(algorithm), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/" + Constants.INPUT_PARAMS)
    public HttpEntity<ParameterDto> addInputParameter(@PathVariable Long id, @RequestBody ParameterDto parameterDto) {
        LOG.debug("Post to retrieve algorithm with id: {}.", id);
        Optional<Algorithm> algorithmOptional = algorithmService.findById(id);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (Objects.isNull(parameterDto.getName())) {
            LOG.error("Received invalid parameter object for post request: {}", parameterDto.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Algorithm algorithm = algorithmOptional.get();
        algorithm.getInputParameters().add(ParameterDto.Converter.convert(parameterDto));
        algorithmService.save(algorithm);
        return new ResponseEntity<>(parameterDto, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/" + Constants.OUTPUT_PARAMS)
    public HttpEntity<ParameterDto> addOutputParameter(@PathVariable Long id, @RequestBody ParameterDto parameterDto) {
        LOG.debug("Post to retrieve algorithm with id: {}.", id);
        Optional<Algorithm> algorithmOptional = algorithmService.findById(id);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (Objects.isNull(parameterDto.getName())) {
            LOG.error("Received invalid parameter object for post request: {}", parameterDto.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Algorithm algorithm = algorithmOptional.get();
        algorithm.getOutputParameters().add(ParameterDto.Converter.convert(parameterDto));
        algorithmService.save(algorithm);
        return new ResponseEntity<>(parameterDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public HttpEntity<AlgorithmDto> getAlgorithm(@PathVariable Long id) {
        LOG.debug("Get to retrieve algorithm with id: {}.", id);

        Optional<Algorithm> algorithmOptional = algorithmService.findById(id);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createAlgorithmDto(algorithmOptional.get()), HttpStatus.OK);
    }

    @GetMapping("/{id}/" + Constants.INPUT_PARAMS)
    public HttpEntity<ParameterListDto> getInputParameters(@PathVariable Long id) {
        LOG.debug("Get to retrieve input parameters for algorithm with id: {}.", id);

        Optional<Algorithm> algorithmOptional = algorithmService.findById(id);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} form the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // convert all output parameters to corresponding dtos
        ParameterListDto parameterListDto = new ParameterListDto();
        parameterListDto.add(algorithmOptional.get().getInputParameters().stream()
                .map(ParameterDto.Converter::convert)
                .collect(Collectors.toList()));

        parameterListDto.add(linkTo(methodOn(AlgorithmController.class).getInputParameters(id)).withSelfRel());
        return new ResponseEntity<>(parameterListDto, HttpStatus.OK);
    }

    @GetMapping("/{id}/" + Constants.OUTPUT_PARAMS)
    public HttpEntity<ParameterListDto> getOutputParameters(@PathVariable Long id) {
        LOG.debug("Get to retrieve output parameters for algorithm with id: {}.", id);

        Optional<Algorithm> algorithmOptional = algorithmService.findById(id);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} form the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // convert all input parameters to corresponding dtos
        ParameterListDto parameterListDto = new ParameterListDto();
        parameterListDto.add(algorithmOptional.get().getOutputParameters().stream()
                .map(ParameterDto.Converter::convert)
                .collect(Collectors.toList()));

        parameterListDto.add(linkTo(methodOn(AlgorithmController.class).getOutputParameters(id)).withSelfRel());
        return new ResponseEntity<>(parameterListDto, HttpStatus.OK);
    }

    @GetMapping("/{id}/" + Constants.NISQ + "/" + Constants.SELECTION_PARAMS)
    public ResponseEntity getSelectionParams(@PathVariable Long id) {
        LOG.debug("Get to retrieve selection parameters for algorithm with Id {} received.", id);

        Optional<Algorithm> algorithmOptional = algorithmService.findById(id);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // determine and return required selection parameters
        List<ParameterDto> requiredParamsDto = nisqAnalyzerService.getRequiredSelectionParameters(algorithmOptional.get())
                .stream()
                .map(ParameterDto.Converter::convert)
                .collect(Collectors.toList());
        ParameterListDto dto = new ParameterListDto(requiredParamsDto);

        // add required links
        dto.add(linkTo(methodOn(AlgorithmController.class).getSelectionParams(id)).withSelfRel());
        dto.add(linkTo(methodOn(AlgorithmController.class).getAlgorithm(id)).withRel(Constants.ALGORITHM));
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PostMapping("/{id}/" + Constants.NISQ + "/" + Constants.SELECTION)
    public ResponseEntity selectImplementations(@PathVariable Long id, @RequestBody ParameterKeyValueDto params) {
        LOG.debug("Post to select implementations for algorithm with Id {} received.", id);

        Optional<Algorithm> algorithmOptional = algorithmService.findById(id);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Algorithm algorithm = algorithmOptional.get();

        if (Objects.isNull(params.getParameters())) {
            LOG.error("Parameter set for the selection is null.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        LOG.debug("Received {} parameters for the selection.", params.getParameters().size());

        // TODO: not all parameters from selection rules and implementations are required, but then the set of possible implementations is reduced
        if (!RestUtils.parametersAvailable(nisqAnalyzerService.getRequiredSelectionParameters(algorithm), params.getParameters())) {
            LOG.error("Parameter set for the selection is not valid.");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Map<Implementation, List<Qpu>> selectionResults;
        try {
            selectionResults = nisqAnalyzerService.performSelection(algorithm, params.getParameters());
        } catch (UnsatisfiedLinkError e) {
            LOG.error("UnsatisfiedLinkError while activating prolog rule. Please make sure prolog is installed and configured correctly to use the NISQ analyzer functionality!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No prolog engine accessible from the server. Selection not possible!");
        }

        // parse suited impl/qpu pairs to dto
        List<SuitableImplQpuPairsDto.ImplQpuPair> implQpuPairs = new ArrayList<>();
        for (Map.Entry<Implementation, List<Qpu>> suitablePair : selectionResults.entrySet()) {
            Implementation implementation = suitablePair.getKey();
            ImplementationDto implementationDto = ImplementationDto.Converter.convert(implementation);
            implementationDto.add(linkTo(methodOn(ImplementationController.class).getImplementation(implementation.getImplementedAlgorithm().getId(), implementation.getId())).withSelfRel());

            List<QpuDto> qpuDtos = new ArrayList<>();
            for (Qpu qpu : suitablePair.getValue()) {
                QpuDto qpuDto = QpuDto.Converter.convert(qpu);
                qpuDto.add(linkTo(methodOn(QpuController.class).getQpu(qpu.getProvider().getId(), qpu.getId())).withSelfRel());
                qpuDtos.add(qpuDto);
            }

            implQpuPairs.add(new SuitableImplQpuPairsDto.ImplQpuPair(implementationDto, qpuDtos));
        }
        SuitableImplQpuPairsDto dto = new SuitableImplQpuPairsDto(implQpuPairs);

        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/{id}/" + Constants.TAGS)
    public HttpEntity<TagListDto> getTags(@PathVariable Long id) {
        Optional<Algorithm> algorithmOptional = algorithmService.findById(id);
        if (!algorithmOptional.isPresent()) {
            LOG.error("Unable to retrieve algorithm with id {} form the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Set<Tag> tags = algorithmOptional.get().getTags();
        TagListDto tagListDto = TagController.createTagDtoList(tags.stream());
        tagListDto.add(linkTo(methodOn(AlgorithmController.class).getTags(id)).withSelfRel());
        return new ResponseEntity<>(tagListDto, HttpStatus.OK);
    }
}
