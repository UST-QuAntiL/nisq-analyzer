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

import javax.transaction.Transactional;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.web.dtos.entities.PrioritizationMethodListDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Tag(name = "xmcda-criteria")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.CRITERIA)
public class XmcdaCriteriaController {

    @Operation(responses = {@ApiResponse(responseCode = "200")}, description = "Get")
    @GetMapping("/")
    @Transactional
    public HttpEntity<PrioritizationMethodListDto> getSupportedPrioritizationMethods() {
        PrioritizationMethodListDto model = new PrioritizationMethodListDto();
        //model.add(analysisJobRepository.findAll().stream().map(this::createAnalysisJobDto).collect(Collectors.toList())); // TODO: add methods
        model.add(linkTo(methodOn(XmcdaCriteriaController.class).getSupportedPrioritizationMethods()).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }
}