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

import org.planqk.atlas.core.model.Sdk;
import org.planqk.atlas.core.services.SdkService;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.dtos.entities.SdkDto;
import org.planqk.atlas.web.dtos.entities.SdkListDto;

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
import org.springframework.test.util.ReflectionTestUtils;
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

@WebMvcTest(SdkController.class)
public class SdkControllerTest {

    @Mock
    private SdkService sdkService;

    @InjectMocks
    private SdkController sdkController;

    private MockMvc mockMvc;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(sdkController).build();
    }

    @Test
    public void contextLoaded() throws Exception {
        assertThat(sdkController).isNotNull();
    }

    private Sdk getTestSdk() {
        Sdk sdk = new Sdk();
        sdk.setName("testsdk");
        return sdk;
    }

    @Test
    public void testGetAllSdks() throws Exception {

        List<Sdk> sdks = new ArrayList<>();
        Sdk sdk = getTestSdk();
        ReflectionTestUtils.setField(sdk, "id", 1L);
        sdks.add(sdk);
        ReflectionTestUtils.setField(sdk, "id", 2L);
        sdks.add(sdk);
        Pageable pageable = PageRequest.of(0, 2);

        Page<Sdk> page = new PageImpl<Sdk>(sdks, pageable, sdks.size());
        when(sdkService.findAll(any(Pageable.class))).thenReturn(page);

        MvcResult mvcResult = mockMvc.perform(get("/" + Constants.SDKS + "/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        SdkListDto sdkListDto = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), SdkListDto.class);
        assertEquals(sdkListDto.getSdkDtos().size(), 2);
    }

    @Test
    public void getSdks_withEmptySdkList() throws Exception {
        when(sdkService.findAll(any(Pageable.class))).thenReturn(Page.empty());
        MvcResult result = mockMvc.perform(get("/" + Constants.SDKS + "/")
                .queryParam(Constants.PAGE, Integer.toString(0))
                .queryParam(Constants.SIZE, Integer.toString(4))
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        SdkListDto sdkListDto = new ObjectMapper().readValue(result.getResponse().getContentAsString(), SdkListDto.class);
        assertEquals(sdkListDto.getSdkDtos().size(), 0);
    }

    @Test
    public void testGetId() throws Exception {
        Sdk sdk = getTestSdk();
        when(sdkService.findById(any(Long.class))).thenReturn(java.util.Optional.of(sdk));

        MvcResult mvcResult = mockMvc.perform(get("/" + Constants.SDKS + "/" + 1 + "/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        SdkDto sdkDto = new ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), SdkDto.class);
        assertEquals(sdkDto.getName(), sdk.getName());
    }

    @Test
    public void testPostSdk() throws Exception {
        Sdk sdk = getTestSdk();
        when(sdkService.save(any(Sdk.class))).thenReturn(sdk);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                .post("/" + Constants.SDKS + "/")
                .content(TestControllerUtils.asJsonString(sdk))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()).andReturn();

        SdkDto createdSdk = new ObjectMapper().readValue(result.getResponse().getContentAsString(), SdkDto.class);
        assertEquals(createdSdk.getName(), sdk.getName());
    }
}
