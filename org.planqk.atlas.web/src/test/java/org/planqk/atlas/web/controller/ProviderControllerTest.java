/********************************************************************************
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
import java.util.Optional;

import org.planqk.atlas.core.model.Algorithm;
import org.planqk.atlas.core.model.Provider;
import org.planqk.atlas.core.services.ProviderService;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.dtos.entities.AlgorithmDto;
import org.planqk.atlas.web.dtos.entities.AlgorithmListDto;
import org.planqk.atlas.web.dtos.entities.ProviderDto;
import org.planqk.atlas.web.dtos.entities.ProviderListDto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProviderControllerTest {

    @Mock
    private ProviderService providerService;

    @InjectMocks
    private ProviderController providerController;

    private MockMvc mockMvc;

    private int page = 0;
    private int size = 2;
    private Pageable pageable = PageRequest.of(page, size);

    @Before
    public void initialize() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(providerController).build();
    }

    @Test
    public void setupTest() {
        assertNotNull(mockMvc);
    }

    @Test
    public void getProviders_withoutPagination() throws Exception {
        when(providerService.findAll(Pageable.unpaged())).thenReturn(Page.empty());
        mockMvc.perform(get("/" + Constants.PROVIDERS + "/")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void getProviders_withEmptyProviderList() throws Exception {
        when(providerService.findAll(pageable)).thenReturn(Page.empty());
        MvcResult result = mockMvc.perform(get("/" + Constants.PROVIDERS + "/")
                .queryParam(Constants.PAGE, Integer.toString(page))
                .queryParam(Constants.SIZE, Integer.toString(size))
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        ProviderListDto providerListDto = new ObjectMapper().readValue(result.getResponse().getContentAsString(), ProviderListDto.class);
        assertEquals(providerListDto.getProviderDtoList().size(), 0);
    }

    @Test
    public void getProviders_withOneProvider() throws Exception {
        List<Provider> providerList = new ArrayList<>();

        Provider provider = new Provider();
        ReflectionTestUtils.setField(provider, "id", 5L);
        providerList.add(provider);

        when(providerService.findAll(pageable)).thenReturn(new PageImpl<>(providerList));

        MvcResult result = mockMvc.perform(get("/" + Constants.PROVIDERS + "/")
                .queryParam(Constants.PAGE, Integer.toString(page))
                .queryParam(Constants.SIZE, Integer.toString(size))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        ProviderListDto providerListDto = new ObjectMapper().readValue(result.getResponse().getContentAsString(), ProviderListDto.class);
        assertEquals(providerListDto.getProviderDtoList().size(), 1);
        assertEquals(providerListDto.getProviderDtoList().get(0).getId(), Long.valueOf(5L));
    }

    @Test
    public void getProvider_returnNotFound() throws Exception {
        mockMvc.perform(get("/" + Constants.PROVIDERS + "/5")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void getProvider_returnProvider() throws Exception {
        Provider provider = new Provider();
        ReflectionTestUtils.setField(provider, "id", 5L);
        when(providerService.findById(5L)).thenReturn(Optional.of(provider));

        MvcResult result = mockMvc.perform(get("/" + Constants.PROVIDERS + "/5")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        ProviderDto response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), ProviderDto.class);
        assertEquals(response.getId(), Long.valueOf(5L));
    }

    @Test
    public void createProvider_returnBadRequest() throws Exception {
        ProviderDto providerDto = new ProviderDto();
        providerDto.setName("IBM");

        mockMvc.perform(post("/" + Constants.PROVIDERS + "/")
                .content(new ObjectMapper().writeValueAsString(providerDto))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }
    
    @Test
    public void createProvider_returnProvider() throws Exception {
        ProviderDto providerDto = new ProviderDto();
        providerDto.setName("IBM");
        providerDto.setAccessKey("123");
        providerDto.setSecretKey("456");
        Provider provider = ProviderDto.Converter.convert(providerDto);
        when(providerService.save(provider)).thenReturn(provider);

        MvcResult result = mockMvc.perform(post("/" + Constants.PROVIDERS + "/")
                .content(new ObjectMapper().writeValueAsString(providerDto))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andReturn();

        ProviderDto response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), ProviderDto.class);
        assertEquals(response.getName(), providerDto.getName());
    }
}
