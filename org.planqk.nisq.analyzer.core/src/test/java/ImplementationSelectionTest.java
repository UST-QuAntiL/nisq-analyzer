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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.planqk.nisq.analyzer.core.Application;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.Algorithm;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.services.AlgorithmService;
import org.planqk.nisq.analyzer.core.services.ImplementationService;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImplementationSelectionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate template;

    @Autowired
    private ImplementationService implementationService;

    @Autowired
    private AlgorithmService algorithmService;


    @BeforeAll
    public void prepareTestDatabase(){

        final String token = "";

        // Create the Shor Algorithm
        Algorithm shorAlg = new Algorithm();
        shorAlg.setName("Shor");
        shorAlg.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N > 0,N odd", "Number to be factorized")
        ));
        shorAlg.setOutputParameters(Arrays.asList(
                new Parameter("Factor", DataType.Integer, "", "")
        ));
        shorAlg = algorithmService.save(shorAlg);

        // Create SDK
        Sdk qiskit = new Sdk();
        qiskit.setName("Qiskit");

        // Create Shor15 Implementation
        Implementation shor15Implementation = new Implementation();
        shor15Implementation.setName("shor-15-qiskit");
        try{
            shor15Implementation.setFileLocation( new URL(
                    "https://github.com/PlanQK/planqk-atlas-content/blob/master/example-data/example-implementations/shor-15-qiskit.py"
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
        shor15Implementation.setImplementedAlgorithm(shorAlg);

        // Create ShorGeneral Implementation
        Implementation shorGeneralImplementation = new Implementation();
        shorGeneralImplementation.setName("shor-general-qiskit");
        try{
            shorGeneralImplementation.setFileLocation( new URL(
                    "https://github.com/PlanQK/planqk-atlas-content/blob/master/example-data/example-implementations/shor-general-qiskit.py"
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
        shorGeneralImplementation.setImplementedAlgorithm(shorAlg);

        // Create Provider
        Provider ibmQ = new Provider();
        ibmQ.setName("IBM");
        ibmQ.setAccessKey("");
        ibmQ.setSecretKey(token);

        // Create IBM QPUs
        Qpu ibmq16 = new Qpu();
        ibmq16.setName("ibmq_16_melbourne");
        ibmq16.setQubitCount(15);
        ibmq16.setT1(50063.8361f);
        ibmq16.setMaxGateTime(1043);
        ibmq16.setSupportedSdks(Arrays.asList(
                qiskit
        ));

        Qpu ibmq5 = new Qpu();
        ibmq5.setName("ibmq_5_yorktown");
        ibmq5.setQubitCount(5);
        ibmq5.setT1(62104.6608f);
        ibmq5.setMaxGateTime(391);
        ibmq5.setSupportedSdks(Arrays.asList(
                qiskit
        ));

        Qpu ibmqsim = new Qpu();
        ibmqsim.setName("ibmq_qasm_simulator");
        ibmqsim.setQubitCount(15);
        ibmqsim.setT1(50063.8361f);
        ibmqsim.setMaxGateTime(1043);
        ibmqsim.setSupportedSdks(Arrays.asList(
                qiskit
        ));
    }

    @Test
    public void testSelection(){

        ResponseEntity<AlgorithmListDto> algs = template.getForEntity("http://localhost:" + port + "/" + Constants.ALGORITHMS + "/", AlgorithmListDto.class);
        Assertions.assertEquals(HttpStatus.OK, algs.getStatusCode());
        Assertions.assertEquals(1, algs.getBody().getAlgorithmDtos().size());
    }
}
