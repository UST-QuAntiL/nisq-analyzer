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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.planqk.nisq.analyzer.core.control.NisqAnalyzerControlService;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.qprov.QProvService;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;


public class ImplementationSelectionTest extends NisqAnalyzerTestCase {

    @MockBean
    private QProvService qProvService;

    @Autowired
    protected AnalysisJobRepository analysisJobRepository;

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

    @Ignore("Ignored by default due to long expected runtime")
    @Test
    public void testSelectionShor15(){

        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job, shorAlgorithmUuid, composeShorInputParameters(15), "");

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
    public void testSelectionShor15PlanQK() {
        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());

        String refreshToken = System.getenv("refresh-token");
        nisqAnalyzerControlService.performSelection(job, shorPlanQKAlgorithmUuid, composeShorInputParameters(15), refreshToken);

        assertContainsImpl(job, "shor15-qiskit-PlanQK");

        assertContainsImplForQpu(job, "shor15-qiskit-PlanQK", "ibmq_qasm_simulator");
        assertContainsImplForQpu(job, "shor15-qiskit-PlanQK", "ibmq_16_melbourne");
    }

    @Ignore("Ignored by default due to long expected runtime")
    @Test
    public void testSelectionShor9(){

        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job, shorAlgorithmUuid, composeShorInputParameters(9), "");

        assertNotContainsImpl(job, "shor15-qiskit");
        assertNotContainsImpl(job, "shor15-pytket");
        assertContainsImpl(job, "shor-general-qiskit");
        assertContainsImpl(job, "shor-general-pytket");

        assertContainsImplForQpu(job, "shor-general-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "shor-general-qiskit", "ibmq_16_melbourne");
        assertContainsImplForQpu(job, "shor-general-pytket", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "shor-general-pytket", "ibmq_16_melbourne");
    }

    @Ignore("Ignored by default due to long expected runtime")
    @Test
    public void testSelectionShor6(){

        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job, shorAlgorithmUuid, composeShorInputParameters(6), "");

        assertNotContainsImpl(job, "shor15-qiskit");
        assertNotContainsImpl(job, "shor15-pytket");
        assertNotContainsImpl(job, "shor-general-qiskit");
        assertNotContainsImpl(job, "shor-general-pytket");
    }

    @Test
    public void testSelectionGroverFix(){
        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job, groverTruthtableUuid, composeGroverInputParameters("0010000000000000"), "");

        assertContainsImpl(job, "grover-fix-truthtable-qiskit");
        assertContainsImpl(job, "grover-general-truthtable-qiskit");

        assertContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_16_melbourne");
    }

    @Test
    public void testSelectionGroverGeneral(){
        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job, groverTruthtableUuid, composeGroverInputParameters("0001000000000000"), "");

        assertNotContainsImpl(job, "grover-fix-truthtable-qiskit");
        assertContainsImpl(job, "grover-general-truthtable-qiskit");

        assertContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_16_melbourne");
    }

    @Test
    public void testSelectionGroverGeneralSmall(){
        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job, groverTruthtableUuid, composeGroverInputParameters("0001"), "");

        assertNotContainsImpl(job, "grover-fix-truthtable-qiskit");
        assertContainsImpl(job, "grover-general-truthtable-qiskit");

        assertContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_qasm_simulator");
        assertContainsImplForQpu(job, "grover-general-truthtable-qiskit", "ibmq_16_melbourne");
    }

    @Test
    public void testSelectionGroverSAT(){
        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job, groverSatUuid, composeGroverSATInputParameters("(A | B) & (A | ~B) & (~A | B)"), "");

        assertContainsImpl(job, "grover-fix-sat-qiskit");
        assertContainsImpl(job, "grover-general-sat-qiskit");

        assertContainsImplForQpu(job, "grover-fix-sat-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "grover-fix-sat-qiskit", "ibmq_16_melbourne");
        assertContainsImplForQpu(job, "grover-general-sat-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "grover-general-sat-qiskit", "ibmq_16_melbourne");
    }

    @Test
    public void testSelectionGroverSATGeneral(){
        AnalysisJob job = analysisJobRepository.save(new AnalysisJob());
        nisqAnalyzerControlService.performSelection(job, groverSatUuid, composeGroverSATInputParameters("(A | B)"), "");

        assertNotContainsImpl(job, "grover-fix-sat-qiskit");
        assertContainsImpl(job, "grover-general-sat-qiskit");

        assertNotContainsImplForQpu(job, "grover-fix-sat-qiskit", "ibmq_qasm_simulator");
        assertNotContainsImplForQpu(job, "grover-fix-sat-qiskit", "ibmq_16_melbourne");
        assertContainsImplForQpu(job, "grover-general-sat-qiskit", "ibmq_qasm_simulator");
        assertContainsImplForQpu(job, "grover-general-sat-qiskit", "ibmq_16_melbourne");
    }
}
