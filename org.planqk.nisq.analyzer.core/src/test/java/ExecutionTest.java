import java.util.Optional;

import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.planqk.nisq.analyzer.core.Application;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.springframework.boot.test.context.SpringBootTest;

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

@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExecutionTest extends NISQTestCase{

    private Qpu qasmSim;


    @BeforeEach
    public void findQasmSimulator(){
        qasmSim = qpuRepository.findAll().stream().filter(qpu -> qpu.getName().contains("simulator")).findFirst().orElseThrow(() -> new IllegalStateException("No QASM Simulator in the database."));
    }

    @Test
    void testShorGeneral(){

        // Try to find the shor general implementation in the database
        Optional<Implementation> shorImpl = implementationRepository.findByImplementedAlgorithm(shorAlgorithmUUID).stream().filter(
                (Implementation impl) -> impl.getName().contains("general")
        ).findFirst();
        Assertions.assertTrue(shorImpl.isPresent());
    }

    @Test
    void testShor15(){

        // Try to find the shor 15 implementation in the database
        Optional<Implementation> shorImpl = implementationRepository.findByImplementedAlgorithm(shorAlgorithmUUID).stream().filter(
                (Implementation impl) -> impl.getName().contains("15")
        ).findFirst();
        Assertions.assertTrue(shorImpl.isPresent());

    }

    @Test
    void testGroverTruthtable(){

        // Try to find the grover truthtable implementation in the database
        Optional<Implementation> groverImpl = implementationRepository.findByImplementedAlgorithm(groverAlgorithmUUID).stream().filter(
                (Implementation impl) -> impl.getName().contains("truthtable")
        ).findFirst();
        Assertions.assertTrue(groverImpl.isPresent());
    }

    @Test
    void testGroverLogicalExpression(){

        // Try to find the grover logical expression implementation in the database
        Optional<Implementation> groverImpl = implementationRepository.findByImplementedAlgorithm(groverAlgorithmUUID).stream().filter(
                (Implementation impl) -> impl.getName().contains("logicalexpression")
        ).findFirst();
        Assertions.assertTrue(groverImpl.isPresent());

    }

}
