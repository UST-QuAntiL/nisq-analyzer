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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.planqk.nisq.analyzer.core.web.dtos.requests.SelectionRequest;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImplementationSelectionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate template;
    
    
    // Services
    @Autowired
    private ImplementationRepository implementationRepository;
    
    @Autowired
    private SdkRepository sdkRepository;
    
    @Autowired
    private QpuRepository qpuRepository;


    // some useful UUIDs
    private UUID shorAlgorithmUUID;

    @BeforeAll
    public void prepareTestDatabase(){

        // Create the Shor Algorithm
        shorAlgorithmUUID = UUID.randomUUID();

        // Create SDK
        Sdk qiskit = new Sdk();
        qiskit.setName("Qiskit");
        qiskit = sdkRepository.save(qiskit);

        // Create Shor15 Implementation
        Implementation shor15Implementation = new Implementation();
        shor15Implementation.setName("shor-15-qiskit");
        try{
            shor15Implementation.setFileLocation( new URL(
                    "https://raw.githubusercontent.com/PlanQK/nisq-analyzer-content/master/example-implementations/shor-15-qiskit.py"
            ));
        }catch (MalformedURLException e){
        }
        shor15Implementation.setSelectionRule("executable(N, shor-15-qiskit) :- N is 15.");
        shor15Implementation.setSdk(qiskit);
        shor15Implementation.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N = 15", "Integer to be factored.")
        ));
        shor15Implementation.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "Phases of N")
        ));
        shor15Implementation.setWidthRule("expectedWidth(W, shor-15-qiskit) :- W is 8.");
        shor15Implementation.setDepthRule("expectedDepth(D, shor-15-qiskit) :- D is 7.");
        shor15Implementation.setImplementedAlgorithm(shorAlgorithmUUID);
        implementationRepository.save(shor15Implementation);

        // Create ShorGeneral Implementation
        Implementation shorGeneralImplementation = new Implementation();
        shorGeneralImplementation.setName("shor-general-qiskit");
        try{
            shorGeneralImplementation.setFileLocation( new URL(
                    "https://raw.githubusercontent.com/PlanQK/nisq-analyzer-content/master/example-implementations/shor-general-qiskit.py"
            ));
        }catch (MalformedURLException e){
        }
        shorGeneralImplementation.setSelectionRule("executable(N, shor-general-qiskit) :- N > 2.");
        shorGeneralImplementation.setSdk(qiskit);
        shorGeneralImplementation.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N > 2", "Integer to be factored")
        ));
        shorGeneralImplementation.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "Phases of N")
        ));
        shorGeneralImplementation.setWidthRule("expectedWidth(W, L, shor-general-qiskit) :- W is 2 * L + 3.");
        shorGeneralImplementation.setDepthRule("expectedDepth(D, L, shor-general-qiskit) :- D is L**3.");
        shorGeneralImplementation.setImplementedAlgorithm(shorAlgorithmUUID);
        implementationRepository.save(shorGeneralImplementation);

        // Create Provider
        // ...

        List<Sdk> supportedSDK = new ArrayList<>();
        supportedSDK.add(qiskit);

        // Create IBM QPUs
        Qpu ibmq16 = new Qpu();
        ibmq16.setName("ibmq_16_melbourne");
        ibmq16.setQubitCount(15);
        ibmq16.setT1(50063.8361f);
        ibmq16.setMaxGateTime(1043);
        ibmq16.setSupportedSdks(supportedSDK);
        qpuRepository.save(ibmq16);

        Qpu ibmq5 = new Qpu();
        ibmq5.setName("ibmq_5_yorktown");
        ibmq5.setQubitCount(5);
        ibmq5.setT1(62104.6608f);
        ibmq5.setMaxGateTime(391);
        ibmq5.setSupportedSdks(Arrays.asList(
                qiskit
        ));
        qpuRepository.save(ibmq5);


        Qpu ibmqsim = new Qpu();
        ibmqsim.setName("ibmq_qasm_simulator");
        ibmqsim.setQubitCount(64);
        ibmqsim.setT1(50063.8361f);
        ibmqsim.setMaxGateTime(1043);
        ibmqsim.setSupportedSdks(Arrays.asList(
                qiskit
        ));
        qpuRepository.save(ibmqsim);
    }

    @Test
    public void testDatabaseEntries(){

        String baseURL = "http://localhost:" + port + "/";

        // Assert two Shor Algorithm implementations in the database
        ResponseEntity<ImplementationListDto> algs = template.getForEntity( baseURL + Constants.IMPLEMENTATIONS + "/?algoId=" + shorAlgorithmUUID.toString(),
                ImplementationListDto.class);
        Assertions.assertEquals(HttpStatus.OK, algs.getStatusCode());
        Assertions.assertEquals(2, algs.getBody().getImplementationDtos().size());

        // Assert one SDK in the database
        ResponseEntity<SdkListDto> sdks = template.getForEntity(baseURL + Constants.SDKS + "/", SdkListDto.class);
        Assertions.assertEquals(HttpStatus.OK, sdks.getStatusCode());
        Assertions.assertEquals(1, sdks.getBody().getSdkDtos().size());

        // Assert three QPUs in the database
        ResponseEntity<QpuListDto> qpus = template.getForEntity(baseURL + Constants.QPUS + "/", QpuListDto.class);
        Assertions.assertEquals(HttpStatus.OK, qpus.getStatusCode());
        Assertions.assertEquals(3, qpus.getBody().getQpuDtoList().size());
    }

    private ResponseEntity<AnalysisResultListDto> performSelection(int N){
        String token = System.getenv("token");
        String baseURL = "http://localhost:" + port + "/";

        // create a request object
        SelectionRequest request = new SelectionRequest();
        request.setAlgorithmId(shorAlgorithmUUID);
        request.setParameters(Map.of(
                "N", Integer.toString(N),
                "L", Integer.toString((int)Math.floor(Math.log(N)/Math.log(2))),
                "token", token
        ));

        ResponseEntity<AnalysisResultListDto> selection = template.postForEntity(baseURL + Constants.SELECTION + "/", request, AnalysisResultListDto.class);
        return selection;
    }

    @Test
    public void testImplementationSelectionN15(){

        ResponseEntity<AnalysisResultListDto> selection = performSelection(15);

        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());
        Assertions.assertEquals(4,selection.getBody().getAnalysisResultList().size());

        for (AnalysisResultDto r : selection.getBody().getAnalysisResultList()) {

            // Assert that the implementation was transpiled using Qiskit service
            Assertions.assertFalse(r.isEstimate(), String.format("%s was not transpiled.", r.getQpu().getName()));

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
    public void testImplementationSelectionN9(){

        ResponseEntity<AnalysisResultListDto> selection = performSelection(9);

        Assertions.assertEquals(HttpStatus.OK, selection.getStatusCode());
        Assertions.assertEquals(2,selection.getBody().getAnalysisResultList().size());

        for (AnalysisResultDto r : selection.getBody().getAnalysisResultList()) {

            // Assert that the implementation was transpiled using Qiskit service
            Assertions.assertFalse(r.isEstimate(), String.format("%s was not transpiled.", r.getQpu().getName()));

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
}
