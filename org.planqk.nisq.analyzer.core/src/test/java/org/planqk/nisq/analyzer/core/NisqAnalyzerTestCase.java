/*******************************************************************************
 * Copyright (c) 2021 University of Stuttgart
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

package org.planqk.nisq.analyzer.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.planqk.nisq.analyzer.core.knowledge.prolog.Constants;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.SdkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = Application.class
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class NisqAnalyzerTestCase {

    @Autowired
    protected ImplementationRepository implementationRepository;

    @Autowired
    protected SdkRepository sdkRepository;

    protected UUID shorAlgorithmUuid = UUID.randomUUID();
    protected UUID shorPlanQKAlgorithmUuid = UUID.randomUUID();
    protected UUID groverTruthtableUuid = UUID.randomUUID();
    protected UUID groverSatUuid = UUID.randomUUID();


    private Sdk qiskitSDK;
    private Sdk forestSDK;
    private Sdk pytketSDK;

    @Before
    public void setUpDatabase(){

        // Setup the SDKs
        qiskitSDK = createSDK("Qiskit");
        forestSDK = createSDK("Forest");
        pytketSDK = createSDK("PyTket");

        try {
            createShorImplementations();
            createGroverImplementations();
        } catch (MalformedURLException e) {
        }
    }

    @After
    public void cleanProlog(){
        Arrays.stream(new File(Constants.basePath).listFiles()).forEach(File::delete);
    }

    @Test
    public void assertDatasetComplete(){

        // Check if SDKs are present
        assertContainsSDK("Qiskit");
        assertContainsSDK("Forest");
        assertContainsSDK("PyTket");

        assertContainsImplementation(shorAlgorithmUuid, "shor15-qiskit");
        assertContainsImplementation(shorAlgorithmUuid, "shor15-pytket");
        assertContainsImplementation(shorAlgorithmUuid, "shor-general-qiskit");
        assertContainsImplementation(shorAlgorithmUuid, "shor-general-pytket");
        assertContainsImplementation(groverTruthtableUuid, "grover-general-truthtable-qiskit");
        assertContainsImplementation(groverTruthtableUuid, "grover-fix-truthtable-qiskit");
        assertContainsImplementation(groverSatUuid, "grover-fix-sat-qiskit");
        assertContainsImplementation(groverSatUuid, "grover-general-sat-qiskit");
    }

    private Sdk createSDK(String name){
        Sdk sdk = new Sdk();
        sdk.setName(name);
        sdkRepository.save(sdk);
        return sdk;
    }

    private void createShorImplementations() throws MalformedURLException{
        Implementation shor15qiskit = new Implementation();
        shor15qiskit.setName("shor15-qiskit");
        shor15qiskit.setImplementedAlgorithm(shorAlgorithmUuid);
        shor15qiskit.setFileLocation(URI.create("https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Shor/shor-fix-15-qiskit.py").toURL());
        shor15qiskit.setSelectionRule("processable(N, shor-fix-15-qiskit) :- N is 15.");
        shor15qiskit.setLanguage("Qiskit");
        shor15qiskit.setSdk(qiskitSDK);
        shor15qiskit.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N = 15", "")
        ));
        shor15qiskit.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "")
        ));
        implementationRepository.save(shor15qiskit);

        Implementation shor15qiskitPlanQK = new Implementation();
        shor15qiskitPlanQK.setName("shor15-qiskit-PlanQK");
        shor15qiskitPlanQK.setImplementedAlgorithm(shorPlanQKAlgorithmUuid);
        shor15qiskitPlanQK.setFileLocation(URI.create("https://platform.planqk.de/qc-catalog/algorithms/e7413acf-c25e-4de8-ab78-75bfc836a839/implementations/1207510f-9007-48b3-93b8-ea51359c0ced/files/1d827208-1976-487e-819b-64df6e990bf3/content").toURL());
        shor15qiskitPlanQK.setSelectionRule("processable(N, shor-fix-15-qiskit) :- N is 15.");
        shor15qiskitPlanQK.setLanguage("Qiskit");
        shor15qiskitPlanQK.setSdk(qiskitSDK);
        shor15qiskitPlanQK.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N = 15", "")
        ));
        shor15qiskitPlanQK.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "")
        ));
        implementationRepository.save(shor15qiskitPlanQK);

        Implementation shor15pytket = new Implementation();
        shor15pytket.setName("shor15-pytket");
        shor15pytket.setImplementedAlgorithm(shorAlgorithmUuid);
        shor15pytket.setFileLocation(URI.create("https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Shor/shor-fix-15-qiskit.py").toURL());
        shor15pytket.setSelectionRule("processable(N, shor-fix-15-pytket) :- N is 15.");
        shor15pytket.setLanguage("Qiskit");
        shor15pytket.setSdk(pytketSDK);
        shor15pytket.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N = 15", "")
        ));
        shor15pytket.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "")
        ));
        implementationRepository.save(shor15pytket);

        Implementation shorGeneralQiskit = new Implementation();
        shorGeneralQiskit.setName("shor-general-qiskit");
        shorGeneralQiskit.setImplementedAlgorithm(shorAlgorithmUuid);
        shorGeneralQiskit.setFileLocation(URI.create("https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Shor/shor-general-qiskit.py").toURL());
        shorGeneralQiskit.setSelectionRule("processable(N, shor-general-qiskit) :- N > 2, 1 is mod(N, 2).");
        shorGeneralQiskit.setLanguage("Qiskit");
        shorGeneralQiskit.setSdk(qiskitSDK);
        shorGeneralQiskit.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N > 2", "")
        ));
        shorGeneralQiskit.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "")
        ));
        implementationRepository.save(shorGeneralQiskit);

        Implementation shorGeneralPytket = new Implementation();
        shorGeneralPytket.setName("shor-general-pytket");
        shorGeneralPytket.setImplementedAlgorithm(shorAlgorithmUuid);
        shorGeneralPytket.setFileLocation(URI.create("https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Shor/shor-general-qiskit.py").toURL());
        shorGeneralPytket.setSelectionRule("processable(N, shor-general-pytket) :- N > 2, 1 is mod(N, 2).");
        shorGeneralPytket.setLanguage("Qiskit");
        shorGeneralPytket.setSdk(pytketSDK);
        shorGeneralPytket.setInputParameters(Arrays.asList(
                new Parameter("N", DataType.Integer, "N > 2", "")
        ));
        shorGeneralPytket.setOutputParameters(Arrays.asList(
                new Parameter("phases", DataType.String, "", "")
        ));
        implementationRepository.save(shorGeneralPytket);
    }

    private void createGroverImplementations() throws MalformedURLException{
        Implementation groverFixTruthtable = new Implementation();
        groverFixTruthtable.setName("grover-fix-truthtable-qiskit");
        groverFixTruthtable.setImplementedAlgorithm(groverTruthtableUuid);
        groverFixTruthtable.setFileLocation(URI.create("https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Grover-Truthtable/grover-fix-truthtable-qiskit.py").toURL());
        groverFixTruthtable.setSelectionRule("processable(Oracle, grover-fix-truthtable-qiskit) :- Oracle = '0010000000000000'.");
        groverFixTruthtable.setLanguage("Qiskit");
        groverFixTruthtable.setSdk(qiskitSDK);
        groverFixTruthtable.setInputParameters(Arrays.asList(
                new Parameter("Oracle", DataType.String, "Oracle = '0010000000000000'", "")
        ));
        groverFixTruthtable.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "")
        ));
        implementationRepository.save(groverFixTruthtable);

        Implementation groverGeneralTruthtable = new Implementation();
        groverGeneralTruthtable.setName("grover-general-truthtable-qiskit");
        groverGeneralTruthtable.setImplementedAlgorithm(groverTruthtableUuid);
        groverGeneralTruthtable.setFileLocation(URI.create("https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Grover-Truthtable/grover-general-truthtable-qiskit.py").toURL());
        groverGeneralTruthtable.setSelectionRule("processable(Oracle, grover-general-truthtable-qiskit) :- Oracle =~ '^[01]+$', atom_length(Oracle, X), X is X /\\ (-X).");
        groverGeneralTruthtable.setLanguage("Qiskit");
        groverGeneralTruthtable.setSdk(qiskitSDK);
        groverGeneralTruthtable.setInputParameters(Arrays.asList(
                new Parameter("Oracle", DataType.String, "Oracle has to be a a binary string of function f in a truth table", "")
        ));
        groverGeneralTruthtable.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "")
        ));
        implementationRepository.save(groverGeneralTruthtable);

        Implementation groverFixSAT = new Implementation();
        groverFixSAT.setName("grover-fix-sat-qiskit");
        groverFixSAT.setImplementedAlgorithm(groverSatUuid);
        groverFixSAT.setFileLocation(URI.create("https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Grover-SAT/grover-fix-sat-qiskit.py").toURL());
        groverFixSAT.setSelectionRule("processable(Formula, grover-fix-sat-qiskit) :- Formula = '(A | B) & (A | ~B) & (~A | B)'.");
        groverFixSAT.setLanguage("Qiskit");
        groverFixSAT.setSdk(qiskitSDK);
        groverFixSAT.setInputParameters(Arrays.asList(
                new Parameter("Formula", DataType.String, "Formula = (A | B) & (A | ~B) & (~A | B)", "")
        ));
        groverFixSAT.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "")
        ));
        implementationRepository.save(groverFixSAT);

        Implementation groverGeneralSAT = new Implementation();
        groverGeneralSAT.setName("grover-general-sat-qiskit");
        groverGeneralSAT.setImplementedAlgorithm(groverSatUuid);
        groverGeneralSAT.setFileLocation(URI.create("https://raw.githubusercontent.com/UST-QuAntiL/nisq-analyzer-content/master/example-implementations/Grover-SAT/grover-general-sat-qiskit.py").toURL());
        groverGeneralSAT.setSelectionRule("processable(Formula, grover-general-sat-qiskit) :- Formula =~ '^[0-9A-Za-z|&()~^ ]+$'.");
        groverGeneralSAT.setLanguage("Qiskit");
        groverGeneralSAT.setSdk(qiskitSDK);
        groverGeneralSAT.setInputParameters(Arrays.asList(
                new Parameter("Formula", DataType.String, "", "")
        ));
        groverGeneralSAT.setOutputParameters(Arrays.asList(
                new Parameter("assignment", DataType.String, "", "")
        ));
        implementationRepository.save(groverGeneralSAT);
    }

    private void assertContainsSDK(String name){
        Assertions.assertTrue(sdkRepository.findByName(name).isPresent(),  "Asserted " + name + " in the database.");
    }

    private void assertContainsImplementation(UUID algorithm, String name){
        Assertions.assertTrue(
                implementationRepository.findByImplementedAlgorithm(algorithm).stream().anyMatch(impl -> impl.getName().equals(name))
        );
    }

    protected Map<String, String> composeShorInputParameters(int n){
        Map<String,String> params = new HashMap<>();
        params.put("N", Integer.toString(n));
        params.put("token", System.getenv("token"));
        return params;
    }

    protected Map<String, String> composeGroverInputParameters(String oracle){
        Map<String,String> params = new HashMap<>();
        params.put("Oracle", oracle);
        params.put("token", System.getenv("token"));
        return params;
    }

    protected Map<String, String> composeGroverSATInputParameters(String formula){
        Map<String,String> params = new HashMap<>();
        params.put("Formula", formula);
        params.put("token", System.getenv("token"));
        return params;
    }

    protected Qpu createDummyQPU(String provider, String name, int qubits, float maxgatetime, float t1){
        Qpu qpu = new Qpu();
        qpu.setId(UUID.randomUUID());
        qpu.setProvider(provider);
        qpu.setName(name);
        qpu.setQubitCount(qubits);
        qpu.setMaxGateTime(maxgatetime);
        qpu.setT1(t1);
        qpu.setSimulator(false);
        return qpu;
    }

    protected Qpu createDummySimulator(String provider, String name, int qubits){
        Qpu qpu = new Qpu();
        qpu.setId(UUID.randomUUID());
        qpu.setProvider(provider);
        qpu.setName(name);
        qpu.setQubitCount(qubits);
        qpu.setSimulator(true);
        return qpu;
    }

    protected void assertContainsImpl(AnalysisJob job, String impl){
        Assertions.assertTrue(job.getJobResults().stream().anyMatch(
                r -> r.getImplementation().getName().contains(impl)
        ));
    }

    protected void assertNotContainsImpl(AnalysisJob job, String impl){
        Assertions.assertFalse(job.getJobResults().stream().anyMatch(
                r -> r.getImplementation().getName().contains(impl)
        ));
    }

    protected void assertContainsImplForQpu(AnalysisJob job, String impl, String qpu){
        Assertions.assertTrue(job.getJobResults().stream().anyMatch(
                r -> r.getImplementation().getName().contains(impl) && r.getQpu().contains(qpu)
        ));
    }

    protected void assertNotContainsImplForQpu(AnalysisJob job, String impl, String qpu){
        Assertions.assertFalse(job.getJobResults().stream().anyMatch(
                r -> r.getImplementation().getName().contains(impl) && r.getQpu().contains(qpu)
        ));
    }
}
