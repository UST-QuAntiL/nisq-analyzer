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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.prioritization.McdaMethod;
import org.planqk.nisq.analyzer.core.repository.xmcda.XmcdaRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.McdaCriterionDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.McdaCriterionListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.McdaMethodDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.McdaMethodListDto;
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
import org.xmcda.v2.Criterion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Tag(name = "xmcda-criteria")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.MCDA_METHODS)
public class XmcdaCriteriaController {

    private final static Logger LOG = LoggerFactory.getLogger(XmcdaCriteriaController.class);

    final private List<McdaMethod> mcdaMethods;

    final private XmcdaRepository xmcdaRepository;

    @Operation(responses = {@ApiResponse(responseCode = "200")}, description = "Get all supported prioritization methods")
    @GetMapping("/")
    public HttpEntity<McdaMethodListDto> getSupportedPrioritizationMethods() {
        LOG.debug("Retrieving all supported MCDA methods!");
        McdaMethodListDto model = new McdaMethodListDto();

        // add all supported methods and corresponding links
        for (McdaMethod mcdaMethod : mcdaMethods) {
            model.add(createMcdaMethodDto(mcdaMethod));
            model.add(linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationMethod(mcdaMethod.getName())).withRel(mcdaMethod.getName()));
        }

        // add self link
        model.add(linkTo(methodOn(XmcdaCriteriaController.class).getSupportedPrioritizationMethods()).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve a single prioritization method")
    @GetMapping("/{methodName}")
    public HttpEntity<McdaMethodDto> getPrioritizationMethod(@PathVariable String methodName) {
        LOG.debug("Retrieving MCDA method with name: {}", methodName);
        Optional<McdaMethod> optional = mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst();

        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createMcdaMethodDto(optional.get()), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve a single prioritization method")
    @GetMapping("/{methodName}/" + Constants.CRITERIA)
    public HttpEntity<McdaCriterionListDto> getCriterionForMethod(@PathVariable String methodName) {
        LOG.debug("Retrieving criteria for MCDA method with name: {}", methodName);
        Optional<McdaMethod> optional = mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst();

        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // get dtos for all criterion defined for this MCDA method
        List<McdaCriterionDto> mcdaCriterionDtos = xmcdaRepository.findByMcdaMethod(methodName).stream()
                .map(this::createMcdaCriterionDto)
                .collect(Collectors.toList());

        McdaCriterionListDto mcdaCriterionListDto = new McdaCriterionListDto();
        mcdaCriterionListDto.add(mcdaCriterionDtos);
        mcdaCriterionListDto.add(linkTo(methodOn(XmcdaCriteriaController.class).getCriterionForMethod(methodName)).withSelfRel());
        return new ResponseEntity<>(mcdaCriterionListDto, HttpStatus.OK);
    }

    private McdaMethodDto createMcdaMethodDto(McdaMethod method) {
        McdaMethodDto dto = new McdaMethodDto();
        dto.setName(method.getName());
        dto.setDescription(method.getDescription());
        dto.add(linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationMethod(method.getName())).withSelfRel());
        dto.add(linkTo(methodOn(XmcdaCriteriaController.class).getCriterionForMethod(method.getName())).withRel(Constants.CRITERIA));
        return dto;
    }

    private McdaCriterionDto createMcdaCriterionDto(Criterion criterion) {
        McdaCriterionDto dto = new McdaCriterionDto();
        dto.setMcdaConcept(criterion.getMcdaConcept());
        dto.setId(criterion.getId());
        dto.setName(criterion.getName());
        dto.setDescription(criterion.getDescription());

        // check if criterion is set to active and return false otherwise
        dto.setActive(
                criterion.getActiveOrScaleOrCriterionFunction().stream()
                        .filter(object -> object instanceof Boolean)
                        .map(object -> (Boolean) object)
                        .findFirst().orElse(false));

        // TODO: add data and links
        return dto;
    }
}
