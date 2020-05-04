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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.planqk.atlas.core.model.Algorithm;
import org.planqk.atlas.core.model.DataType;
import org.planqk.atlas.core.model.Parameter;
import org.planqk.atlas.core.services.AlgorithmService;
import org.planqk.atlas.nisq.analyzer.control.NisqAnalyzerControlService;
import org.planqk.atlas.web.Constants;
import org.planqk.atlas.web.dtos.entities.AlgorithmDto;
import org.planqk.atlas.web.dtos.entities.AlgorithmListDto;
import org.planqk.atlas.web.dtos.entities.ParameterListDto;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AlgorithmControllerTest {

    @Mock
    private NisqAnalyzerControlService nisqService;

    @Mock
    private AlgorithmService algorithmService;

    @InjectMocks
    private AlgorithmController algorithmController;

    private MockMvc mockMvc;

    private int page = 0;
    private int size = 2;
    private Pageable pageable = PageRequest.of(page, size);

    @Before
    public void initialize() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(algorithmController).build();
    }

    @Test
    public void setupTest() {
        assertNotNull(mockMvc);
    }

    @Test
    public void getAlgorithms_withoutPagination() throws Exception {
        when(algorithmService.findAll(Pageable.unpaged())).thenReturn(Page.empty());
        mockMvc.perform(get("/" + Constants.ALGORITHMS + "/")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void getAlgorithms_withEmptyAlgorithmList() throws Exception {
        when(algorithmService.findAll(pageable)).thenReturn(Page.empty());
        MvcResult result = mockMvc.perform(get("/" + Constants.ALGORITHMS + "/")
                .queryParam(Constants.PAGE, Integer.toString(page))
                .queryParam(Constants.SIZE, Integer.toString(size))
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        AlgorithmListDto algorithmListDto = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AlgorithmListDto.class);
        assertEquals(0, algorithmListDto.getAlgorithmDtos().size());
    }

    @Test
    public void getAlgorithms_withTwoAlgorithmList() throws Exception {
        List<Algorithm> algorithmList = new ArrayList<>();

        Algorithm algorithm1 = new Algorithm();
        ReflectionTestUtils.setField(algorithm1, "id", 1L);
        algorithmList.add(algorithm1);

        Algorithm algorithm2 = new Algorithm();
        ReflectionTestUtils.setField(algorithm2, "id", 2L);
        algorithmList.add(algorithm2);

        when(algorithmService.findAll(pageable)).thenReturn(new PageImpl<>(algorithmList));

        MvcResult result = mockMvc.perform(get("/" + Constants.ALGORITHMS + "/")
                .queryParam(Constants.PAGE, Integer.toString(page))
                .queryParam(Constants.SIZE, Integer.toString(size))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        AlgorithmListDto algorithmListDto = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AlgorithmListDto.class);
        assertEquals(2, algorithmListDto.getAlgorithmDtos().size());
    }

    @Test
    public void getAlgorithm_returnNotFound() throws Exception {
        mockMvc.perform(get("/" + Constants.ALGORITHMS + "/5")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void getAlgorithm_returnAlgorithm() throws Exception {
        Algorithm algorithm = new Algorithm();
        ReflectionTestUtils.setField(algorithm, "id", 5L);
        when(algorithmService.findById(5L)).thenReturn(Optional.of(algorithm));

        MvcResult result = mockMvc.perform(get("/" + Constants.ALGORITHMS + "/5")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        AlgorithmDto response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AlgorithmDto.class);
        assertEquals(response.getId(), Long.valueOf(5L));
    }

    @Test
    public void getInputParameters_returnNotFound() throws Exception {
        mockMvc.perform(get("/" + Constants.ALGORITHMS + "/5/" + Constants.INPUT_PARAMS)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void getInputParameters_returnParameters() throws Exception {
        List<Parameter> parameterList = new ArrayList<>();
        parameterList.add(new Parameter("param1", DataType.String, null, "First parameter"));
        parameterList.add(new Parameter("param2", DataType.String, null, "Second parameter"));

        Algorithm algorithm = new Algorithm();
        ReflectionTestUtils.setField(algorithm, "id", 5L);
        algorithm.setInputParameters(parameterList);

        when(algorithmService.findById(5L)).thenReturn(Optional.of(algorithm));

        MvcResult result = mockMvc.perform(get("/" + Constants.ALGORITHMS + "/5/" + Constants.INPUT_PARAMS)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        ParameterListDto response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), ParameterListDto.class);
        assertEquals(2, response.getParameters().size());
    }

    @Test
    public void getOutputParameters_returnNotFound() throws Exception {
        mockMvc.perform(get("/" + Constants.ALGORITHMS + "/5/" + Constants.OUTPUT_PARAMS)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void getOutputParameters_returnParameters() throws Exception {
        List<Parameter> parameterList = new ArrayList<>();
        parameterList.add(new Parameter("param1", DataType.String, null, "First parameter"));
        parameterList.add(new Parameter("param2", DataType.String, null, "Second parameter"));
        parameterList.add(new Parameter("param3", DataType.String, null, "Third parameter"));

        Algorithm algorithm = new Algorithm();
        ReflectionTestUtils.setField(algorithm, "id", 5L);
        algorithm.setInputParameters(parameterList);

        when(algorithmService.findById(5L)).thenReturn(Optional.of(algorithm));

        MvcResult result = mockMvc.perform(get("/" + Constants.ALGORITHMS + "/5/" + Constants.INPUT_PARAMS)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        ParameterListDto response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), ParameterListDto.class);
        assertEquals(3, response.getParameters().size());
    }

    @Test
    public void createAlgorithm_returnBadRequest() throws Exception {
        AlgorithmDto algorithmDto = new AlgorithmDto();
        mockMvc.perform(post("/" + Constants.ALGORITHMS + "/")
                .content(new ObjectMapper().writeValueAsString(algorithmDto))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    public void createAlgorithm_returnAlgorithm() throws Exception {
        AlgorithmDto algorithmDto = new AlgorithmDto();
        algorithmDto.setName("Shor");
        Algorithm algorithm = AlgorithmDto.Converter.convert(algorithmDto);
        when(algorithmService.save(algorithm)).thenReturn(algorithm);

        MvcResult result = mockMvc.perform(post("/" + Constants.ALGORITHMS + "/")
                .content(new ObjectMapper().writeValueAsString(algorithmDto))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andReturn();

        AlgorithmDto response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), AlgorithmDto.class);
        assertEquals(response.getName(), algorithmDto.getName());
    }

    @Test
    public void getSelectionParameters_returnNotFound() throws Exception {
        mockMvc.perform(get("/" + Constants.ALGORITHMS + "/5/" + Constants.NISQ + "/" + Constants.SELECTION_PARAMS)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void getSelectionParameters_returnParameters() throws Exception {
        Set<Parameter> parameterSet = new HashSet<>();
        parameterSet.add(new Parameter("param1", DataType.String, null, "First parameter"));
        parameterSet.add(new Parameter("param2", DataType.String, null, "Second parameter"));
        parameterSet.add(new Parameter("param3", DataType.String, null, "Third parameter"));
        when(nisqService.getRequiredSelectionParameters(any(Algorithm.class))).thenReturn(parameterSet);

        Algorithm algorithm = new Algorithm();
        ReflectionTestUtils.setField(algorithm, "id", 5L);
        algorithm.setInputParameters(new ArrayList<>(parameterSet));
        when(algorithmService.findById(any(Long.class))).thenReturn(Optional.of(algorithm));

        MvcResult result = mockMvc.perform(get("/" + Constants.ALGORITHMS + "/5/" + Constants.NISQ + "/" + Constants.SELECTION_PARAMS)
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        ParameterListDto response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), ParameterListDto.class);
        assertEquals(3, response.getParameters().size());
    }
}
