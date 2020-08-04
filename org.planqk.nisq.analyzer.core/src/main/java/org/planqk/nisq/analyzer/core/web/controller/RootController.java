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

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.control.NisqAnalyzerControlService;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Root controller to access all entities within Quality, trigger the hardware selection, and execution of quantum
 * algorithms.
 */
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
public class RootController {

    private final static Logger LOG = LoggerFactory.getLogger(QpuController.class);

    private final NisqAnalyzerControlService nisqAnalyzerService;

    public RootController(NisqAnalyzerControlService nisqAnalyzerService) {
        this.nisqAnalyzerService = nisqAnalyzerService;
    }

    @GetMapping("/")
    public HttpEntity<RepresentationModel> root() {
        RepresentationModel responseEntity = new RepresentationModel<>();

        // add links to sub-controllers
        responseEntity.add(linkTo(methodOn(RootController.class).root()).withSelfRel());
        responseEntity.add(linkTo(methodOn(ImplementationController.class).getImplementations(null)).withRel(Constants.IMPLEMENTATIONS));
        responseEntity.add(linkTo(methodOn(QpuController.class).getQpus()).withRel(Constants.QPUS));
        responseEntity.add(linkTo(methodOn(SdkController.class).getSdks()).withRel(Constants.SDKS));
        responseEntity.add(linkTo(methodOn(RootController.class).getSelectionParams(null)).withRel(Constants.SELECTION_PARAMS))
        ;

        return new ResponseEntity<>(responseEntity, HttpStatus.OK);
    }

    @GetMapping("/" + Constants.SELECTION_PARAMS)
    public ResponseEntity getSelectionParams(@RequestParam UUID algoId) {
        LOG.debug("Get to retrieve selection parameters for algorithm with Id {} received.", algoId);

        if (Objects.isNull(algoId)) {
            LOG.error("AlgoId have to be provided to get selection parameters!");
            return new ResponseEntity<>("AlgoId have to be provided to get selection parameters!", HttpStatus.BAD_REQUEST);
        }

        // determine and return required selection parameters
        List<ParameterDto> requiredParamsDto = nisqAnalyzerService.getRequiredSelectionParameters(algoId)
                .stream()
                .map(ParameterDto.Converter::convert)
                .collect(Collectors.toList());
        ParameterListDto dto = new ParameterListDto(requiredParamsDto);

        // add required links
        dto.add(linkTo(methodOn(RootController.class).getSelectionParams(algoId)).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}
