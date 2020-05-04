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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.planqk.atlas.core.model.Algorithm;
import org.planqk.atlas.core.model.Implementation;
import org.planqk.atlas.core.model.Tag;
import org.planqk.atlas.core.services.TagService;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.dtos.entities.AlgorithmDto;
import org.planqk.atlas.web.dtos.entities.AlgorithmListDto;
import org.planqk.atlas.web.dtos.entities.ImplementationListDto;
import org.planqk.atlas.web.dtos.entities.TagDto;
import org.planqk.atlas.web.dtos.entities.TagListDto;
import org.planqk.atlas.web.utils.RestUtils;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

//
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.TAGS)
public class TagController {

    private TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    public static TagListDto createTagDtoList(Stream<Tag> tagStream) {
        TagListDto tagListDto = new TagListDto();
        tagListDto.add(tagStream.map(tag -> createTagDto(tag)).collect(Collectors.toList()));
        tagListDto.add(linkTo(methodOn(TagController.class).getTags(null, null)).withRel(Constants.TAGS));
        return tagListDto;
    }

    /**
     * Create a DTO object for a given {@link Tag} with the contained data and the links to related objects.
     *
     * @param tag the {@link Tag} to create the DTO for
     * @return the created DTO
     */
    public static TagDto createTagDto(Tag tag) {
        TagDto dto = TagDto.Converter.convert(tag);
        dto.add(linkTo(methodOn(TagController.class).getTagById(tag.getId())).withSelfRel());
        dto.add(linkTo(methodOn(TagController.class).getAlgorithmsOfTag(tag.getId())).withRel(Constants.ALGORITHMS));
        dto.add(linkTo(methodOn(TagController.class).getImplementationsOfTag(tag.getId())).withRel(Constants.IMPLEMENTATIONS));
        return dto;
    }

    @GetMapping(value = "/")
    HttpEntity<TagListDto> getTags(@RequestParam(required = false) Integer page,
                                   @RequestParam(required = false) Integer size) {
        Page<Tag> tags = this.tagService.findAll(RestUtils.getPageableFromRequestParams(page, size));
        TagListDto dtoList = createTagDtoList(tags.stream());
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @PostMapping(value = "/")
    HttpEntity<TagDto> createTag(@RequestBody Tag tag) {
        TagDto savedTag = TagDto.Converter.convert(this.tagService.save(tag));

        savedTag.add(linkTo(methodOn(TagController.class).getTagById(savedTag.getId())).withSelfRel());
        return new ResponseEntity<>(savedTag, HttpStatus.CREATED);
    }

    @GetMapping(value = "/{tagId}")
    HttpEntity<TagDto> getTagById(@PathVariable Long tagId) {
        Optional<Tag> tagOptional = this.tagService.getTagById(tagId);
        if (!tagOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(createTagDto(tagOptional.get()), HttpStatus.OK);
    }

    @GetMapping(value = "/{tagId}/" + Constants.ALGORITHMS)
    HttpEntity<AlgorithmListDto> getAlgorithmsOfTag(@PathVariable Long tagId) {
        Optional<Tag> tagOptional = this.tagService.getTagById(tagId);
        if (!tagOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Set<Algorithm> algorithms = tagOptional.get().getAlgorithms();
        AlgorithmListDto algorithmListDto = new AlgorithmListDto();
        List<AlgorithmDto> algorithmsDto = algorithms.stream().map(AlgorithmController::createAlgorithmDto).collect(Collectors.toList());
        algorithmListDto.add(algorithmsDto);
        algorithmListDto.add(linkTo(methodOn(TagController.class).getAlgorithmsOfTag(tagId)).withSelfRel());
        return new ResponseEntity<>(algorithmListDto, HttpStatus.OK);
    }

    @GetMapping(value = "/{tagId}/" + Constants.IMPLEMENTATIONS)
    HttpEntity<ImplementationListDto> getImplementationsOfTag(@PathVariable Long tagId) {
        Optional<Tag> tagOptional = this.tagService.getTagById(tagId);
        if (!tagOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Set<Implementation> implementations = tagOptional.get().getImplementations();
        ImplementationListDto implementationListDto = new ImplementationListDto();
        implementationListDto.add(implementations.stream().map(ImplementationController::createImplementationDto).collect(Collectors.toList()));
        implementationListDto.add(linkTo(methodOn(TagController.class).getImplementationsOfTag(tagId)).withSelfRel());
        return new ResponseEntity<>(implementationListDto, HttpStatus.OK);
    }
}
