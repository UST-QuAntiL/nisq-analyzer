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

import org.planqk.atlas.web.Constants;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/")
    public HttpEntity<RepresentationModel> root() {
        RepresentationModel responseEntity = new RepresentationModel<>();

        // add links to sub-controllers
        responseEntity.add(linkTo(methodOn(RootController.class).root()).withSelfRel());
        responseEntity.add(linkTo(methodOn(AlgorithmController.class).getAlgorithms(Constants.DEFAULT_PAGE_NUMBER, Constants.DEFAULT_PAGE_SIZE)).withRel(Constants.ALGORITHMS));
        responseEntity.add(linkTo(methodOn(ProviderController.class).getProviders(Constants.DEFAULT_PAGE_NUMBER, Constants.DEFAULT_PAGE_SIZE)).withRel(Constants.PROVIDERS));
        responseEntity.add(linkTo(methodOn(SdkController.class).getSdks(Constants.DEFAULT_PAGE_NUMBER, Constants.DEFAULT_PAGE_SIZE)).withRel(Constants.SDKS));
        responseEntity.add(linkTo(methodOn(TagController.class).getTags(Constants.DEFAULT_PAGE_NUMBER, Constants.DEFAULT_PAGE_SIZE)).withRel(Constants.TAGS));

        return new ResponseEntity<>(responseEntity, HttpStatus.OK);
    }
}
