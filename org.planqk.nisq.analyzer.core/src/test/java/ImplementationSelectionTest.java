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
import org.planqk.nisq.analyzer.core.web.dtos.entities.ImplementationListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.SdkListDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.SelectionRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.event.annotation.BeforeTestClass;

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

    private ResponseEntity<AnalysisResultListDto> performGroverSATSelection(String formula){
        String token = System.getenv("token");
        String baseURL = "http://localhost:" + port + "/";

        // create a request object
        SelectionRequestDto request = new SelectionRequestDto();
        request.setAlgorithmId(groverSATAlgorithmUUID);
        request.setParameters(NISQTestCase.inputParameters(
                "Formula", formula,
                "token", token
        ));

        ResponseEntity<AnalysisResultListDto> selection = template.postForEntity(baseURL + Constants.SELECTION + "/", request, AnalysisResultListDto.class);
        return selection;
    }

    private ResponseEntity<AnalysisResultListDto> performGroverTruthtableSelection(String oracle){
        String token = System.getenv("token");
        String baseURL = "http://localhost:" + port + "/";

        // create a request object
        SelectionRequestDto request = new SelectionRequestDto();
        request.setAlgorithmId(groverTruthtableAlgorithmUUID);
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

        assertConsistentAnalysisResultList(selection.getBody());

        // Assert implementations
        assertImpl("shor-general", selection.getBody());
        assertImpl("shor-fix-15", selection.getBody());

        // Assert specific QPUs
        assertImplQPUPair("shor-general", "ibmq_qasm", selection.getBody());
        assertNotImplQPUPair("shor-general", "ibmq_16", selection.getBody());
        assertNotImplQPUPair("shor-general", "ibmq_5", selection.getBody());
        assertImplQPUPair("shor-fix-15", "ibmq_qasm", selection.getBody());
        assertImplQPUPair("shor-fix-15", "ibmq_16", selection.getBody());
        assertImplQPUPair("shor-fix-15", "ibmq_5", selection.getBody());

        // Assert result count
        Assertions.assertEquals(4,selection.getBody().getAnalysisResultList().size());
    }

    @Test
    public void testImplSelectionShorN9(){

        ResponseEntity<AnalysisResultListDto> selection = performShorSelection(9);
        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());

        assertConsistentAnalysisResultList(selection.getBody());

        // Assert implementations
        assertImpl("shor-general", selection.getBody());
        assertNotImpl("shor-fix-15", selection.getBody());

        // Assert specific QPUs
        assertImplQPUPair("shor-general", "ibmq_qasm", selection.getBody());
        assertNotImplQPUPair("shor-general", "ibmq_16", selection.getBody());
        assertNotImplQPUPair("shor-general", "ibmq_5", selection.getBody());

        // Assert result count
        Assertions.assertEquals(1,selection.getBody().getAnalysisResultList().size());
    }

    @Test
    public void testImplSelectionGroverSat(){

        ResponseEntity<AnalysisResultListDto> selection = performGroverSATSelection("(A | B) & (A | ~B) & (~A | B)");
        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());

        assertConsistentAnalysisResultList(selection.getBody());

        // Assert implementations
        assertImpl("grover-general-sat", selection.getBody());
        assertImpl("grover-fix-sat", selection.getBody());
        assertNotImpl("grover-general-truthtable", selection.getBody());
        assertNotImpl("grover-fix-truthtable", selection.getBody());

        // Assert specific QPUs
        assertImplQPUPair("grover-fix-sat", "ibmq_16", selection.getBody());
        assertImplQPUPair("grover-fix-sat", "ibmq_qasm", selection.getBody());
        assertNotImplQPUPair("grover-fix-sat", "ibmq_5", selection.getBody());
        assertImplQPUPair("grover-general-sat", "ibmq_16", selection.getBody());
        assertImplQPUPair("grover-general-sat", "ibmq_qasm", selection.getBody());
        assertNotImplQPUPair("grover-general-sat", "ibmq_5", selection.getBody());

        // Assert result count
        Assertions.assertEquals(4,selection.getBody().getAnalysisResultList().size());
    }

    @Test
    public void testImplSelectionGroverTruthtable(){

        ResponseEntity<AnalysisResultListDto> selection = performGroverTruthtableSelection("00000001");
        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());

        assertConsistentAnalysisResultList(selection.getBody());

        // Assert implementations
        assertNotImpl("grover-general-sat", selection.getBody());
        assertNotImpl("grover-fix-sat", selection.getBody());
        assertImpl("grover-general-truthtable", selection.getBody());
        assertNotImpl("grover-fix-truthtable", selection.getBody());

        // Assert specific QPUs
        assertNotImplQPUPair("grover-general-truthtable", "ibmq_16", selection.getBody()); // too deep
        assertImplQPUPair("grover-general-truthtable", "ibmq_5", selection.getBody());
        assertImplQPUPair("grover-general-truthtable", "ibmq_qasm", selection.getBody());

        // Assert result count
        Assertions.assertEquals(2,selection.getBody().getAnalysisResultList().size());
    }
}
