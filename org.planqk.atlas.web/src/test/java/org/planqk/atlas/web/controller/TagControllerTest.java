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

import org.planqk.atlas.core.model.Tag;
import org.planqk.atlas.core.services.TagService;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.dtos.entities.TagDto;
import org.planqk.atlas.web.dtos.entities.TagListDto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
public class TagControllerTest {

    @Mock
    private TagService tagService;

    @InjectMocks
    private TagController tagController;

    private MockMvc mockMvc;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(tagController).build();
    }

    @Test
    public void contextLoaded() throws Exception {
        assertThat(tagController).isNotNull();
    }

    private Tag getTestTag() {
        Tag tag1 = new Tag();
        tag1.setKey("testkey");
        tag1.setValue("testvalue");
        return tag1;
    }

    @Test
    public void testGetAllTags() throws Exception {
        List<Tag> tags = new ArrayList<>();
        Tag tag1 = getTestTag();
        tags.add(tag1);
        tags.add(new Tag());
        Pageable pageable = PageRequest.of(0, 2);

        Page<Tag> page = new PageImpl<Tag>(tags, pageable, tags.size());
        when(tagService.findAll(any(Pageable.class))).thenReturn(page);

        MvcResult mvcResult = mockMvc.perform(get("/" + Constants.TAGS + "/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        TagListDto tagList = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), TagListDto.class);
        assertEquals(tagList.getTagsDtos().size(), 2);
    }

    @Test
    public void getTags_withEmptyTagList() throws Exception {
        when(tagService.findAll(any(Pageable.class))).thenReturn(Page.empty());
        MvcResult result = mockMvc.perform(get("/" + Constants.TAGS + "/")
                .queryParam(Constants.PAGE, Integer.toString(0))
                .queryParam(Constants.SIZE, Integer.toString(4))
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        TagListDto tagListDto = new ObjectMapper().readValue(result.getResponse().getContentAsString(), TagListDto.class);
        assertEquals(tagListDto.getTagsDtos().size(), 0);
    }

    @Test
    public void testGetId() throws Exception {
        Tag tag1 = getTestTag();
        when(tagService.getTagById(any(Long.class))).thenReturn(java.util.Optional.of(tag1));

        MvcResult mvcResult = mockMvc.perform(get("/" + Constants.TAGS + "/" + 1 + "/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        TagDto createdTag = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), TagDto.class);
        assertEquals(createdTag.getKey(), tag1.getKey());
        assertEquals(createdTag.getValue(), tag1.getValue());
    }

    @Test
    public void testPostTag() throws Exception {
        Tag tag1 = getTestTag();
        when(tagService.save(tag1)).thenReturn(tag1);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                .post("/" + Constants.TAGS + "/")
                .content(TestControllerUtils.asJsonString(tag1))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()).andReturn();

        TagDto createdTag = new ObjectMapper().readValue(result.getResponse().getContentAsString(), TagDto.class);
        assertEquals(createdTag.getKey(), tag1.getKey());
        assertEquals(createdTag.getValue(), tag1.getValue());
    }
}
