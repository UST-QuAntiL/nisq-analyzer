import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.QpuRepository;
import org.planqk.nisq.analyzer.core.repository.SdkRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ImplementationListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.SdkListDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

public class NISQTestCase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate template;


    // Services
    @Autowired
    protected ImplementationRepository implementationRepository;

    @Autowired
    protected SdkRepository sdkRepository;

    @Autowired
    protected QpuRepository qpuRepository;


    // some useful UUIDs
    protected UUID shorAlgorithmUUID;
    protected UUID groverAlgorithmUUID;

    @AfterAll
    public void tearDownTestDocker(){
        try {
            Runtime.getRuntime().exec("docker stop planqk-test");
        }catch (Exception e){

        }
    }

    private Sdk createQiskitSDK(){
        // Create SDK
        Sdk qiskit = new Sdk();
        qiskit.setName("Qiskit");
        qiskit = sdkRepository.save(qiskit);

        return qiskit;
    }

    private void createGroverImplementations(Sdk qiskit){

        Implementation groverGeneralLogic = new Implementation();
        groverGeneralLogic.setName("grover-general-logicalexpression-qiskit");
        try{
            groverGeneralLogic.setFileLocation(new URL(
                    "https://raw.githubusercontent.com/PlanQK/nisq-analyzer-content/master/example-implementations/grover-general-logicalexpression-qiskit.py"
            ));
        }catch (MalformedURLException e){}
        groverGeneralLogic.setSelectionRule("executable(Oracle, grover-general-logicalexpression-qiskit) :- not(Oracle == null).");
        groverGeneralLogic.setSdk(qiskit);
        groverGeneralLogic.setInputParameters( Arrays.asList(
                new Parameter("Oracle", DataType.String, "Oracle has to be a Boolean function", "Oracle for grover")
        ));
        groverGeneralLogic.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "Assignment of Boolean variables such that f evaluates to true")
        ));
        groverGeneralLogic.setWidthRule("expectedWidth(W, grover-general-logicalexpression-qiskit) :- W >= 2.");
        groverGeneralLogic.setDepthRule("expectedDepth(D, grover-general-logicalexpression-qiskit) :- D >= 5.");
        groverGeneralLogic.setImplementedAlgorithm(groverAlgorithmUUID);
        implementationRepository.save(groverGeneralLogic);

        Implementation groverGeneralTruthtable = new Implementation();
        groverGeneralTruthtable.setName("grover-general-truthtable-qiskit");
        try{
            groverGeneralTruthtable.setFileLocation(new URL(
                    "https://raw.githubusercontent.com/PlanQK/nisq-analyzer-content/master/example-implementations/grover-general-truthtable-qiskit.py"
            ));
        }catch (MalformedURLException e){}
        groverGeneralTruthtable.setSelectionRule("executable(Oracle, grover-general-truthtable-qiskit) :- not(Oracle == null).");
        groverGeneralTruthtable.setSdk(qiskit);
        groverGeneralTruthtable.setInputParameters(Arrays.asList(
                new Parameter("Oracle", DataType.String, "Oracle has to be a a binary string of function f in a truth table", "Truth table oracle for grover")
        ));
        groverGeneralTruthtable.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "Assignment of Boolean variables such that f evaluates to true")
        ));
        groverGeneralTruthtable.setWidthRule("expectedWidth(W, grover-general-truthtable-qiskit) :- W >= 2.");
        groverGeneralTruthtable.setDepthRule("expectedDepth(D, grover-general-truthtable-qiskit) :- D >= 5.");
        groverGeneralTruthtable.setImplementedAlgorithm(groverAlgorithmUUID);
        implementationRepository.save(groverGeneralTruthtable);
    }

    private void createShorImplementations(Sdk qiskit){
        // Create Shor15 Implementation
        Implementation shor15Implementation = new Implementation();
        shor15Implementation.setName("shor-15-qiskit");
        try{
            shor15Implementation.setFileLocation( new URL(
                    "https://raw.githubusercontent.com/PlanQK/nisq-analyzer-content/master/example-implementations/shor-15-qiskit.py"
            ));
        }catch (MalformedURLException e){}
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
    }

    private void createQPUs(Sdk qiskit){
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

    @BeforeAll
    public void prepareTestDatabase(){

        // Create the Qiskit SDK
        Sdk qiskit = createQiskitSDK();

        // Create the Shor Algorithm and its implementations
        shorAlgorithmUUID = UUID.randomUUID();
        createShorImplementations(qiskit);

        // Create the Gover Algorithm and its implementations
        groverAlgorithmUUID = UUID.randomUUID();
        createGroverImplementations(qiskit);

        // Create the IBM QPUs
        createQPUs(qiskit);

    }

    @Test
    public void testDatabaseEntries(){

        String baseURL = "http://localhost:" + port + "/";

        // Assert two Shor Algorithm implementations in the database
        {
            ResponseEntity<ImplementationListDto> algs = template.getForEntity(baseURL + Constants.IMPLEMENTATIONS + "/?algoId=" + shorAlgorithmUUID.toString(),
                    ImplementationListDto.class);
            Assertions.assertEquals(HttpStatus.OK, algs.getStatusCode());
            Assertions.assertEquals(2, algs.getBody().getImplementationDtos().size());
        }

        // Assert two Grover Algorithm implementations in the database
        {
            ResponseEntity<ImplementationListDto> algs = template.getForEntity(baseURL + Constants.IMPLEMENTATIONS + "/?algoId=" + groverAlgorithmUUID.toString(),
                    ImplementationListDto.class);
            Assertions.assertEquals(HttpStatus.OK, algs.getStatusCode());
            Assertions.assertEquals(2, algs.getBody().getImplementationDtos().size());
        }

        // Assert one SDK in the database
        ResponseEntity<SdkListDto> sdks = template.getForEntity(baseURL + Constants.SDKS + "/", SdkListDto.class);
        Assertions.assertEquals(HttpStatus.OK, sdks.getStatusCode());
        Assertions.assertEquals(1, sdks.getBody().getSdkDtos().size());

        // Assert three QPUs in the database
        ResponseEntity<QpuListDto> qpus = template.getForEntity(baseURL + Constants.QPUS + "/", QpuListDto.class);
        Assertions.assertEquals(HttpStatus.OK, qpus.getStatusCode());
        Assertions.assertEquals(3, qpus.getBody().getQpuDtoList().size());
    }

}
