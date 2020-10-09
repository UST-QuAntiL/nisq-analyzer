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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.crypto.Data;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.planqk.nisq.analyzer.core.Application;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.QpuRepository;
import org.planqk.nisq.analyzer.core.repository.SdkRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultListDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.SelectionRequestDto;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImplementationSelectionTest extends NISQTestCase {

    private ResponseEntity<AnalysisResultListDto> performShorSelection(int N){
        String token = System.getenv("token");
        String baseURL = "http://localhost:" + port + "/";

        // create a request object
        SelectionRequestDto request = new SelectionRequestDto();
        request.setAlgorithmId(shorAlgorithmUUID);
        request.setParameters(NISQTestCase.inputParameters(
                "N", Integer.toString(N),
                "L", Integer.toString((int)Math.floor(Math.log(N)/Math.log(2))),
                "token", token
        ));

        ResponseEntity<AnalysisResultListDto> selection = template.postForEntity(baseURL + Constants.SELECTION + "/", request, AnalysisResultListDto.class);
        return selection;
    }

    private ResponseEntity<AnalysisResultListDto> performGroverSelection(String oracle){
        String token = System.getenv("token");
        String baseURL = "http://localhost:" + port + "/";

        // create a request object
        SelectionRequestDto request = new SelectionRequestDto();
        request.setAlgorithmId(groverAlgorithmUUID);
        request.setParameters(NISQTestCase.inputParameters(
                "Oracle", oracle,
                "token", token
        ));

        ResponseEntity<AnalysisResultListDto> selection = template.postForEntity(baseURL + Constants.SELECTION + "/", request, AnalysisResultListDto.class);
        return selection;
    }

    @Test
    public void testImplSelectionShorN15(){

        ResponseEntity<AnalysisResultListDto> selection = performShorSelection(15);

        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());
        Assertions.assertEquals(3,selection.getBody().getAnalysisResultList().size());

        for (AnalysisResultDto r : selection.getBody().getAnalysisResultList()) {
            // Check transpiled width is always less than the number of available Qbits
            Assertions.assertTrue(r.getAnalysedWidth() <= r.getQpu().getNumberOfQubits(), r.getQpu().getName());
        }

        Assertions.assertTrue(selection.getBody().getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains("general")
        ), "Shor General not in the result list.");

        Assertions.assertTrue(selection.getBody().getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains("shor-15")
        ), "Shor 15 not in the result list.");
    }

    @Test
    public void testImplSelectionShorN9(){

        ResponseEntity<AnalysisResultListDto> selection = performShorSelection(9);

        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());
        Assertions.assertEquals(1,selection.getBody().getAnalysisResultList().size());

        for (AnalysisResultDto r : selection.getBody().getAnalysisResultList()) {
            // Check transpiled width is always less than the number of available Qbits
            Assertions.assertTrue(r.getAnalysedWidth() <= r.getQpu().getNumberOfQubits(), r.getQpu().getName());
        }

        Assertions.assertTrue(selection.getBody().getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains("general")
        ), "Shor General not in the result list.");

        Assertions.assertFalse(selection.getBody().getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains("shor-15")
        ), "Shor 15 in the result list.");
    }

    @Test
    public void testImplSelectionGroverLogic(){

        ResponseEntity<AnalysisResultListDto> selection = performGroverSelection("(A | B) & (A | ~B) & (~A | B)");
        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());
        Assertions.assertEquals(1, selection.getBody().getAnalysisResultList().size());

        for (AnalysisResultDto r : selection.getBody().getAnalysisResultList()) {
            // Check transpiled width is always less than the number of available Qbits
            Assertions.assertTrue(r.getAnalysedWidth() <= r.getQpu().getNumberOfQubits(), r.getQpu().getName());
        }

        Assertions.assertTrue(selection.getBody().getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains("grover-general-logicalexpression")
        ), "Grover General Logical Expression not in the result list.");

        Assertions.assertFalse(selection.getBody().getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains("grover-general-truthtable")
        ), "Grover General Truthtable in the result list.");
    }

    @Test
    public void testImplSelectionGroverTruthtable(){

        ResponseEntity<AnalysisResultListDto> selection = performGroverSelection("00000001");
        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());
        Assertions.assertEquals(2, selection.getBody().getAnalysisResultList().size());

        for (AnalysisResultDto r : selection.getBody().getAnalysisResultList()) {
            // Check transpiled width is always less than the number of available Qbits
            Assertions.assertTrue(r.getAnalysedWidth() <= r.getQpu().getNumberOfQubits(), r.getQpu().getName());
        }

        Assertions.assertTrue(selection.getBody().getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains("grover-general-truthtable")
        ), "Grover General Truthtable not in the result list.");

        Assertions.assertFalse(selection.getBody().getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains("grover-general-logicalexpression")
        ), "Grover General Logical Expression in the result list.");

    }
}
