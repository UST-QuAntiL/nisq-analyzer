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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.planqk.nisq.analyzer.core.Application;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AlgorithmListDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ImplementationSelectionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate template;

    @Test
    public void testSelection(){

        ResponseEntity<AlgorithmListDto> algs = template.getForEntity("http://localhost:" + port + "/" + Constants.ALGORITHMS + "/", AlgorithmListDto.class);
        Assertions.assertEquals(HttpStatus.OK, algs.getStatusCode());
    }
}
