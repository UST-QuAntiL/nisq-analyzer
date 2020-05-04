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

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.services.ProviderService;
import org.planqk.nisq.analyzer.core.utils.RestUtils;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ProviderDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ProviderListDto;
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
 * Controller to access and manipulate quantum computing providers.
 */
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.PROVIDERS)
public class ProviderController {

    final private static Logger LOG = LoggerFactory.getLogger(ProviderController.class);
    private ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping("/")
    public HttpEntity<ProviderListDto> getProviders(@RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        LOG.debug("Get to retrieve all providers received.");
        ProviderListDto providerListDto = new ProviderListDto();

        // add all available providers to the response
        for (Provider provider : providerService.findAll(RestUtils.getPageableFromRequestParams(page, size))) {
            providerListDto.add(createProviderDto(provider));
            providerListDto.add(linkTo(methodOn(ProviderController.class).getProvider(provider.getId())).
                    withRel(provider.getId().toString()));
        }

        providerListDto.add(linkTo(methodOn(ProviderController.class).getProviders(Constants.DEFAULT_PAGE_NUMBER,
                Constants.DEFAULT_PAGE_SIZE)).withSelfRel());
        return new ResponseEntity<>(providerListDto, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public HttpEntity<ProviderDto> getProvider(@PathVariable Long id) {
        LOG.debug("Get to retrieve provider with id: {}.", id);

        Optional<Provider> providerOptional = providerService.findById(id);
        if (!providerOptional.isPresent()) {
            LOG.error("Unable to retrieve provider with id {} from the repository.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createProviderDto(providerOptional.get()), HttpStatus.OK);
    }

    @PostMapping("/")
    public HttpEntity<ProviderDto> createProvider(@RequestBody ProviderDto providerDto) {
        LOG.debug("Post to create new provider received.");

        if (Objects.isNull(providerDto.getName()) || Objects.isNull(providerDto.getAccessKey())
                || Objects.isNull(providerDto.getSecretKey())) {
            LOG.error("Received invalid provider object for post request: {}", providerDto.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Provider provider = providerService.save(ProviderDto.Converter.convert(providerDto));
        return new ResponseEntity<>(createProviderDto(provider), HttpStatus.CREATED);
    }

    /**
     * Create a DTO object for a given {@link Provider} with the contained data and the links to related objects.
     *
     * @param provider the {@link Provider} to create the DTO for
     * @return the created DTO
     */
    private ProviderDto createProviderDto(Provider provider) {
        ProviderDto providerDto = ProviderDto.Converter.convert(provider);
        providerDto.add(linkTo(methodOn(ProviderController.class).getProvider(provider.getId())).withSelfRel());
        providerDto.add(linkTo(methodOn(QpuController.class).getQpus(provider.getId(), Constants.DEFAULT_PAGE_NUMBER,
                Constants.DEFAULT_PAGE_SIZE)).withRel(Constants.QPUS));
        return providerDto;
    }
}
