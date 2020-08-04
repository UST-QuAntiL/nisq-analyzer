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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.knowledge.prolog.PrologFactUpdater;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.repository.QpuRepository;
import org.planqk.nisq.analyzer.core.repository.SdkRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuListDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.CreateQpuRequest;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller to access and manipulate quantum processing units (QPUs).
 */
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.QPUS)
public class QpuController {

    private final static Logger LOG = LoggerFactory.getLogger(QpuController.class);
    private final QpuRepository qpuRepository;
    private final SdkRepository sdkRepository;
    private final PrologFactUpdater prologFactUpdater;

    public QpuController(QpuRepository qpuRepository, SdkRepository sdkRepository, PrologFactUpdater prologFactUpdater) {
        this.qpuRepository = qpuRepository;
        this.sdkRepository = sdkRepository;
        this.prologFactUpdater = prologFactUpdater;
    }

    @GetMapping("/")
    public HttpEntity<QpuListDto> getQpus() {
        LOG.debug("Get to retrieve all QPUs received.");
        QpuListDto qpuListDto = new QpuListDto();

        // add all available algorithms to the response
        for (Qpu qpu : qpuRepository.findAll()) {
            qpuListDto.add(createQpuDto(qpu));
            qpuListDto.add(linkTo(methodOn(QpuController.class).getQpu(qpu.getId())).withRel(qpu.getId().toString()));
        }

        qpuListDto.add(linkTo(methodOn(QpuController.class).getQpus()).withSelfRel());
        return new ResponseEntity<>(qpuListDto, HttpStatus.OK);
    }

    @GetMapping("/{qpuId}")
    public HttpEntity<QpuDto> getQpu(@PathVariable Long qpuId) {
        LOG.debug("Get to retrieve QPU with id: {}.", qpuId);

        Optional<Qpu> qpuOptional = qpuRepository.findById(qpuId);
        if (!qpuOptional.isPresent()) {
            LOG.error("Unable to retrieve QPU with id {} from the repository.", qpuId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createQpuDto(qpuOptional.get()), HttpStatus.OK);
    }

    @PostMapping("/")
    public HttpEntity<QpuDto> createQpu(@RequestBody CreateQpuRequest qpuRequest) {
        LOG.debug("Post to create new QPU received.");

        // check consistency of the QPU object
        if (Objects.isNull(qpuRequest.getName())) {
            LOG.error("Received invalid QPU object for post request: {}", qpuRequest.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // add supported Sdks in case there are some defined
        List<Sdk> supportedSdks = new ArrayList<>();
        if (Objects.nonNull(qpuRequest.getSupportedSdkIds())) {
            LOG.debug("Supported SDKs are defined for the QPU.");
            for (Long sdkId : qpuRequest.getSupportedSdkIds()) {
                Optional<Sdk> sdkOptional = sdkRepository.findById(sdkId);
                if (!sdkOptional.isPresent()) {
                    LOG.error("Unable to retrieve SDK with id {} from the repository.", sdkId);
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
                supportedSdks.add(sdkOptional.get());
            }
        }

        // store and return QPU
        Qpu qpu = qpuRepository.save(QpuDto.Converter.convert(qpuRequest, supportedSdks));
        prologFactUpdater.handleQpuInsertion(qpu);
        return new ResponseEntity<>(createQpuDto(qpu), HttpStatus.OK);
    }

    /**
     * Create a DTO object for a given {@link Qpu}.
     *
     * @param qpu the {@link Qpu} to create the DTO for
     * @return the created DTO
     */
    private QpuDto createQpuDto(Qpu qpu) {
        QpuDto qpuDto = QpuDto.Converter.convert(qpu);
        qpuDto.add(linkTo(methodOn(QpuController.class).getQpu(qpu.getId())).withSelfRel());
        for (Sdk sdk : qpu.getSupportedSdks()) {
            qpuDto.add(linkTo(methodOn(SdkController.class).getSdk(sdk.getId())).withRel(Constants.SUPPORTED_SDK + "-" + sdk.getId()));
        }
        return qpuDto;
    }
}
