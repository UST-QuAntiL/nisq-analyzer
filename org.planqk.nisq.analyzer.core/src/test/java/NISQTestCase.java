import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultListDto;
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
    protected UUID groverSATAlgorithmUUID;
    protected UUID groverTruthtableAlgorithmUUID;

    @AfterAll
    public void tearDownTestDocker(){
        try {
            Runtime.getRuntime().exec("docker stop planqk-test");
        }catch (Exception e){

        }
    }

    protected static Map<String,String> inputParameters(String... v) {
        Map<String, String> para = new HashMap<>();

        for (int i = 0; i < v.length; i += 2) {
            para.put(v[i], v[i + 1]);
        }
        return para;
    }

    private Sdk createQiskitSDK(){
        // Create SDK
        Sdk qiskit = new Sdk();
        qiskit.setName("Qiskit");
        qiskit = sdkRepository.save(qiskit);

        return qiskit;
    }

    private void createGroverImplementations(Sdk qiskit){

        // Grover General SAT
        //------------------------------------------------------------------------
        Implementation groverGeneralSat = new Implementation();
        groverGeneralSat.setName("grover-general-sat-qiskit");
        try{
            groverGeneralSat.setFileLocation(new URL(
                    "https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Grover-SAT/grover-general-sat-qiskit.py"
            ));
        }catch (MalformedURLException e){}
        groverGeneralSat.setSelectionRule("processable(Formula, grover-general-sat-qiskit) :- Formula =~ '^[0-9A-Za-z|&()~^ ]+$'.");
        groverGeneralSat.setSdk(qiskit);
        groverGeneralSat.setInputParameters( Arrays.asList(
                new Parameter("Formula", DataType.String, "Formula has to be a Boolean function", "Oracle for grover")
        ));
        groverGeneralSat.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "Assignment of Boolean variables such that f evaluates to true")
        ));
        groverGeneralSat.setImplementedAlgorithm(groverSATAlgorithmUUID);
        implementationRepository.save(groverGeneralSat);
        //------------------------------------------------------------------------

        // Grover Fixed SAT
        //------------------------------------------------------------------------
        Implementation groverFixSat = new Implementation();
        groverFixSat.setName("grover-fix-sat-qiskit");
        try{
            groverFixSat.setFileLocation(new URL(
                    "https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Grover-SAT/grover-fix-sat-qiskit.py"
            ));
        }catch (MalformedURLException e){}
        groverFixSat.setSelectionRule("processable(Formula, grover-fix-sat-qiskit) :- Formula = '(A | B) & (A | ~B) & (~A | B)'.");
        groverFixSat.setSdk(qiskit);
        groverFixSat.setInputParameters( Arrays.asList(
                new Parameter("Formula", DataType.String, "Formula = (A | B) & (A | ~B) & (~A | B)", "Oracle for grover")
        ));
        groverFixSat.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "Assignment of Boolean variables such that f evaluates to true")
        ));
        groverFixSat.setImplementedAlgorithm(groverSATAlgorithmUUID);
        implementationRepository.save(groverFixSat);
        //------------------------------------------------------------------------

        // Grover General Truthtable
        //------------------------------------------------------------------------
        Implementation groverGeneralTruthtable = new Implementation();
        groverGeneralTruthtable.setName("grover-general-truthtable-qiskit");
        try{
            groverGeneralTruthtable.setFileLocation(new URL(
                    "https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Grover-Truthtable/grover-general-truthtable-qiskit.py"
            ));
        }catch (MalformedURLException e){}
        groverGeneralTruthtable.setSelectionRule("processable(Oracle, grover-general-truthtable-qiskit) :- not(Oracle == null).");
        groverGeneralTruthtable.setSdk(qiskit);
        groverGeneralTruthtable.setInputParameters(Arrays.asList(
                new Parameter("Oracle", DataType.String, "Oracle has to be a a binary string of function f in a truth table", "Truth table oracle for grover")
        ));
        groverGeneralTruthtable.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "Assignment of Boolean variables such that f evaluates to true")
        ));
        groverGeneralTruthtable.setImplementedAlgorithm(groverTruthtableAlgorithmUUID);
        implementationRepository.save(groverGeneralTruthtable);
        //------------------------------------------------------------------------

        // Grover Fixed Truthtable
        //------------------------------------------------------------------------
        Implementation groverFixTruthtable = new Implementation();
        groverFixTruthtable.setName("grover-fix-truthtable-qiskit");
        try{
            groverFixTruthtable.setFileLocation(new URL(
                    "https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Grover-Truthtable/grover-fix-truthtable-qiskit.py"
            ));
        }catch (MalformedURLException e){}
        groverFixTruthtable.setSelectionRule("processable(Oracle, grover-fix-truthtable-qiskit) :- Oracle = '0010000000000000'.");
        groverFixTruthtable.setSdk(qiskit);
        groverFixTruthtable.setInputParameters(Arrays.asList(
                new Parameter("Oracle", DataType.String, "Oracle = '0010000000000000'", "Truth table oracle for grover")
        ));
        groverFixTruthtable.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "Assignment of Boolean variables such that f evaluates to true")
        ));
        groverFixTruthtable.setImplementedAlgorithm(groverTruthtableAlgorithmUUID);
        implementationRepository.save(groverFixTruthtable);
        //------------------------------------------------------------------------
    }

    private void createShorImplementations(Sdk qiskit){
        // Shor 15
        //------------------------------------------------------------------------
        Implementation shor15Implementation = new Implementation();
        shor15Implementation.setName("shor-fix-15-qiskit");
        try{
            shor15Implementation.setFileLocation( new URL(
                    "https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Shor/shor-fix-15-qiskit.py"
            ));
        }catch (MalformedURLException e){}
        shor15Implementation.setSelectionRule("processable(N, shor-fix-15-qiskit) :- N is 15.");
        shor15Implementation.setSdk(qiskit);
        shor15Implementation.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N = 15", "Integer to be factored.")
        ));
        shor15Implementation.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "Phases of N")
        ));
        shor15Implementation.setImplementedAlgorithm(shorAlgorithmUUID);
        implementationRepository.save(shor15Implementation);
        //------------------------------------------------------------------------

        // Shor General
        //------------------------------------------------------------------------
        Implementation shorGeneralImplementation = new Implementation();
        shorGeneralImplementation.setName("shor-general-qiskit");
        try{
            shorGeneralImplementation.setFileLocation( new URL(
                    "https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Shor/shor-general-qiskit.py"
            ));
        }catch (MalformedURLException e){
        }
        shorGeneralImplementation.setSelectionRule("processable(N, shor-general-qiskit) :- N > 2, 1 is mod(N, 2).");
        shorGeneralImplementation.setSdk(qiskit);
        shorGeneralImplementation.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N > 2", "Integer to be factored")
        ));
        shorGeneralImplementation.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "Phases of N")
        ));
        shorGeneralImplementation.setImplementedAlgorithm(shorAlgorithmUUID);
        implementationRepository.save(shorGeneralImplementation);
        //------------------------------------------------------------------------
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
        ibmqsim.setT1(1000000.0f); // dummy values for the simulator
        ibmqsim.setMaxGateTime(1); // dummy values for the simulator
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
        groverSATAlgorithmUUID = UUID.randomUUID();
        groverTruthtableAlgorithmUUID = UUID.randomUUID();
        createGroverImplementations(qiskit);

        // Create the IBM QPUs
        createQPUs(qiskit);

    }

    public void assertImpl(String impl, AnalysisResultListDto results){
        Assertions.assertTrue(results.getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains(impl)
        ), impl + " not in the result list.");
    }

    public void assertNotImpl(String impl, AnalysisResultListDto results){
        Assertions.assertFalse(results.getAnalysisResultList().stream().anyMatch(
                r -> r.getImplementation().getName().contains(impl)
        ), impl + " in the result list.");
    }

    public void assertImplQPUPair(String impl, String qpu, AnalysisResultListDto results){
        Assertions.assertTrue(results.getAnalysisResultList().stream().anyMatch(
                r -> (r.getImplementation().getName().contains(impl) && r.getQpu().getName().contains(qpu))
        ), "(" + impl + " | " + qpu + ") not in the result list.");
    }

    public void assertNotImplQPUPair(String impl, String qpu, AnalysisResultListDto results){
        Assertions.assertFalse(results.getAnalysisResultList().stream().anyMatch(
                r -> (r.getImplementation().getName().contains(impl) && r.getQpu().getName().contains(qpu))
        ), "(" + impl + " | " + qpu + ") in the result list.");
    }

    public void assertConsistentAnalysisResultList(AnalysisResultListDto results){

        for (AnalysisResultDto r : results.getAnalysisResultList()) {

            // Assert that the implementation was transpiled using Qiskit service
            Assertions.assertFalse(r.isEstimate(), String.format("%s was not transpiled.", r.getQpu().getName()));

            // Check transpiled width is always less than the number of available Qbits
            Assertions.assertTrue(r.getAnalysedWidth() <= r.getQpu().getNumberOfQubits(), String.format("QBits exceeded on %s." ,r.getQpu().getName()));
        }
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

        // Assert two Grover SAT Algorithm implementations in the database
        {
            ResponseEntity<ImplementationListDto> algs = template.getForEntity(baseURL + Constants.IMPLEMENTATIONS + "/?algoId=" + groverSATAlgorithmUUID.toString(),
                    ImplementationListDto.class);
            Assertions.assertEquals(HttpStatus.OK, algs.getStatusCode());
            Assertions.assertEquals(2, algs.getBody().getImplementationDtos().size());
        }

        // Assert two Grover Truthtable Algorithm implementations in the database
        {
            ResponseEntity<ImplementationListDto> algs = template.getForEntity(baseURL + Constants.IMPLEMENTATIONS + "/?algoId=" + groverTruthtableAlgorithmUUID.toString(),
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
