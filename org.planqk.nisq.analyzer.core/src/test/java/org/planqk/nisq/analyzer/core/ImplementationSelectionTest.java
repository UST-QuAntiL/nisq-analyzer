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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.tomcat.jni.Time;
import org.hibernate.service.spi.InjectService;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.planqk.nisq.analyzer.core.Application;
import org.planqk.nisq.analyzer.core.connector.SdkConnector;
import org.planqk.nisq.analyzer.core.connector.qiskit.QiskitSdkConnector;
import org.planqk.nisq.analyzer.core.control.NisqAnalyzerControlService;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.qprov.QProvService;
import org.planqk.nisq.analyzer.core.repository.ImplementationRepository;
import org.planqk.nisq.analyzer.core.repository.ImplementationSelectionJobRepository;
import org.planqk.nisq.analyzer.core.repository.SdkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;


public class ImplementationSelectionTest extends NisqAnalyzerTestCase {

    @MockBean
    private QProvService qProvService;

    @Autowired
    protected ImplementationSelectionJobRepository implementationSelectionJobRepository;

    @Autowired
    protected NisqAnalyzerControlService nisqAnalyzerControlService;

    @Before
    public void setUpQProvMock(){

        Provider ibmq = new Provider();
        ibmq.setName("IBMQ");

        Mockito.when(qProvService.getProviders()).thenReturn(Arrays.asList(ibmq));
        Mockito.when(qProvService.getQPUs(ibmq)).thenReturn(Arrays.asList(
                createDummyQPU("IBMQ", "ibmq_16_melbourne", 15, 1696, 54502.2906f),
                createDummySimulator("IBMQ", "ibmq_qasm_simulator", 32)
        ));
    }

    @Test
    public void testQProvMocking(){
        Assertions.assertTrue(qProvService.getProviders().stream().allMatch(p -> p.getName().equals("IBMQ")));
    }

    @Test
    public void testSelectionShor15(){

        AnalysisJob job = implementationSelectionJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job,shorAlgorithmUuid, composeShorInputParameters(15));

        assertContainsImpl(job, "shor15-qiskit");
        assertContainsImpl(job, "shor15-pytket");
        assertContainsImpl(job, "shor-general-qiskit");
        assertContainsImpl(job, "shor-general-pytket");

        assertContainsImplForQpu(job, "shor15-qiskit", "ibmq_qasm_simulator");
        assertContainsImplForQpu(job, "shor15-qiskit", "ibmq_16_melbourne");
        assertContainsImplForQpu(job, "shor15-pytket", "ibmq_qasm_simulator");
        assertContainsImplForQpu(job, "shor15-pytket", "ibmq_16_melbourne");
        assertContainsImplForQpu(job, "shor-general-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "shor-general-qiskit", "ibmq_16_melbourne");
        assertContainsImplForQpu(job, "shor-general-pytket", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "shor-general-pytket", "ibmq_16_melbourne");
    }

    @Test
    public void testSelectionShor9(){

        AnalysisJob job = implementationSelectionJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job,shorAlgorithmUuid, composeShorInputParameters(9));

        assertNotContainsImpl(job, "shor15-qiskit");
        assertNotContainsImpl(job, "shor15-pytket");
        assertContainsImpl(job, "shor-general-qiskit");
        assertContainsImpl(job, "shor-general-pytket");

        assertContainsImplForQpu(job, "shor-general-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "shor-general-qiskit", "ibmq_16_melbourne");
        assertContainsImplForQpu(job, "shor-general-pytket", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "shor-general-pytket", "ibmq_16_melbourne");
    }

    @Test
    public void testSelectionShor6(){

        AnalysisJob job = implementationSelectionJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job,shorAlgorithmUuid, composeShorInputParameters(6));

        assertNotContainsImpl(job, "shor15-qiskit");
        assertNotContainsImpl(job, "shor15-pytket");
        assertNotContainsImpl(job, "shor-general-qiskit");
        assertNotContainsImpl(job, "shor-general-pytket");
    }

    @Test
    public void testSelectionGroverFix(){
        AnalysisJob job = implementationSelectionJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job,groverTruthtableUuid, composeGroverInputParameters("0010000000000000"));

        assertContainsImpl(job, "grover-fix-truthtable-qiskit");
        assertContainsImpl(job, "grover-general-truthtable-qiskit");

        assertContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_16_melbourne");
    }

    @Test
    public void testSelectionGroverGeneral(){
        AnalysisJob job = implementationSelectionJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job,groverTruthtableUuid, composeGroverInputParameters("0001000000000000"));

        assertNotContainsImpl(job, "grover-fix-truthtable-qiskit");
        assertContainsImpl(job, "grover-general-truthtable-qiskit");

        assertContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_16_melbourne");
    }

    @Test
    public void testSelectionGroverGeneralSmall(){
        AnalysisJob job = implementationSelectionJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job,groverTruthtableUuid, composeGroverInputParameters("0001"));

        assertNotContainsImpl(job, "grover-fix-truthtable-qiskit");
        assertContainsImpl(job, "grover-general-truthtable-qiskit");

        assertContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_qasm_simulator");
        assertContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_16_melbourne");
    }
}
