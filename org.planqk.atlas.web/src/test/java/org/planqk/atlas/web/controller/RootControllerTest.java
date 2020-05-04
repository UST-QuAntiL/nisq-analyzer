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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class RootControllerTest {

    @InjectMocks
    private RootController rootController;

    private MockMvc mockMvc;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(rootController).build();
    }

    @Test
    public void contextLoaded() throws Exception {
        assertThat(rootController).isNotNull();
    }

    @Test
    public void testGetHateoasLinks() throws Exception {

        MvcResult result = mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        RepresentationModel response = new ObjectMapper().readValue(result.getResponse().getContentAsString(), RepresentationModel.class);
        assertTrue(response.getLinks().hasSize(5L));

        assertTrue(response.getLinks().hasLink("self"));
        assertTrue(response.getLinks().hasLink(Constants.ALGORITHMS));
        assertTrue(response.getLinks().hasLink(Constants.PROVIDERS));
        assertTrue(response.getLinks().hasLink(Constants.SDKS));
        assertTrue(response.getLinks().hasLink(Constants.TAGS));
        assertFalse(response.getLinks().hasLink("randomLink"));
    }
}
