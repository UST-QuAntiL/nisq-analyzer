import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import ch.qos.logback.core.CoreConstants;
import com.sun.istack.Pool;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.planqk.nisq.analyzer.core.Application;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ExecutionResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.ExecutionRequest;
import org.planqk.nisq.analyzer.core.web.dtos.requests.SelectionRequest;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SimulatorExecutionTest extends NISQTestCase{

    private Qpu qasmSim;


    @BeforeEach
    public void findQasmSimulator(){
        qasmSim = qpuRepository.findAll().stream().filter(qpu -> qpu.getName().contains("simulator")).findFirst().orElseThrow(() -> new IllegalStateException("No QASM Simulator in the database."));
    }

    private ResponseEntity<ExecutionResultDto> execute(Implementation implementation, Qpu qpu, Map<String,String> parameters){
        String token = System.getenv("token");
        String baseURL = "http://localhost:" + port + "/";

        // Append the token to the parameters
        Map<String,String> p = new HashMap<>(parameters);
        p.putIfAbsent("token", token);

        // create a request object
        ExecutionRequest request = new ExecutionRequest();
        request.setQpuId(qpu.getId());
        request.setAnalysedDepth(0);
        request.setAnalysedWidth(0);
        request.setParameters(p);

        ResponseEntity<ExecutionResultDto> result = template.postForEntity(
                baseURL + Constants.IMPLEMENTATIONS + "/" + implementation.getId().toString() + "/" + Constants.EXECUTION,
                request, ExecutionResultDto.class);
        return result;
    }

    private UUID assertExecutionInitialized(ResponseEntity<ExecutionResultDto> result){
        Assertions.assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
        ExecutionResultDto dto = result.getBody();
        Assertions.assertEquals(ExecutionResultStatus.INITIALIZED, dto.getStatus());
        return dto.getId();
    }

    private ExecutionResultDto getResultStatus(Implementation implementation, UUID resultID){
        String baseURL = "http://localhost:" + port + "/";

        ResponseEntity<ExecutionResultDto> result = template.getForEntity(
                baseURL + Constants.IMPLEMENTATIONS + "/" + implementation.getId() + "/" + Constants.RESULTS + "/" + resultID + "/"
                ,ExecutionResultDto.class);

        Assertions.assertTrue(result.getStatusCode().is2xxSuccessful());
        return result.getBody();
    }

    private ExecutionResultDto waitForTermination(Implementation implementation, UUID resultID){

        ExecutionResultDto result;

        do {
            // wait
            try{
                Thread.sleep(15000);
            }catch (InterruptedException e){

            }finally {
                // Request the result status
                result = getResultStatus(implementation, resultID);
            }
        }while(result.getStatus().equals(ExecutionResultStatus.INITIALIZED) || result.getStatus().equals(ExecutionResultStatus.RUNNING));

        return result;
    }

    @Test
    void testShorGeneral(){

        // Try to find the shor general implementation in the database
        Optional<Implementation> shorImpl = implementationRepository.findByImplementedAlgorithm(shorAlgorithmUUID).stream().filter(
                (Implementation impl) -> impl.getName().contains("general")
        ).findFirst();
        Assertions.assertTrue(shorImpl.isPresent());

        // Start execution
        ResponseEntity<ExecutionResultDto> result = execute(shorImpl.get(), qasmSim, Map.of(
                "N", "9",
                "L", "4"
        ));
        UUID resultID = assertExecutionInitialized(result);
        ExecutionResultDto finalResult = waitForTermination(shorImpl.get(), resultID);

        Assertions.assertEquals(ExecutionResultStatus.FINISHED, finalResult.getStatus());
        System.out.println(finalResult.getResult());
        Assertions.assertTrue(finalResult.getResult().contains("counts"));
    }

    @Test
    void testShor15(){

        // Try to find the shor 15 implementation in the database
        Optional<Implementation> shorImpl = implementationRepository.findByImplementedAlgorithm(shorAlgorithmUUID).stream().filter(
                (Implementation impl) -> impl.getName().contains("15")
        ).findFirst();
        Assertions.assertTrue(shorImpl.isPresent());

        // Start execution
        ResponseEntity<ExecutionResultDto> result = execute(shorImpl.get(), qasmSim, Map.of(
                "N", "15"
        ));
        UUID resultID = assertExecutionInitialized(result);
        ExecutionResultDto finalResult = waitForTermination(shorImpl.get(), resultID);

        Assertions.assertEquals(ExecutionResultStatus.FINISHED, finalResult.getStatus());
        System.out.println(finalResult.getResult());
        Assertions.assertTrue(finalResult.getResult().contains("counts"));
    }

    @Test
    void testGroverTruthtable(){

        // Try to find the grover truthtable implementation in the database
        Optional<Implementation> groverImpl = implementationRepository.findByImplementedAlgorithm(groverAlgorithmUUID).stream().filter(
                (Implementation impl) -> impl.getName().contains("truthtable")
        ).findFirst();
        Assertions.assertTrue(groverImpl.isPresent());

        // Start execution
        ResponseEntity<ExecutionResultDto> result = execute(groverImpl.get(), qasmSim, Map.of(
                "Oracle", "00000001"
        ));
        UUID resultID = assertExecutionInitialized(result);
        ExecutionResultDto finalResult = waitForTermination(groverImpl.get(), resultID);

        Assertions.assertEquals(ExecutionResultStatus.FINISHED, finalResult.getStatus());
        System.out.println(finalResult.getResult());
        Assertions.assertTrue(finalResult.getResult().contains("counts"));
    }

    @Test
    void testGroverLogicalExpression(){

        // Try to find the grover logical expression implementation in the database
        Optional<Implementation> groverImpl = implementationRepository.findByImplementedAlgorithm(groverAlgorithmUUID).stream().filter(
                (Implementation impl) -> impl.getName().contains("logicalexpression")
        ).findFirst();
        Assertions.assertTrue(groverImpl.isPresent());

        // Start execution
        ResponseEntity<ExecutionResultDto> result = execute(groverImpl.get(), qasmSim, Map.of(
                "Oracle", "(A | B) & (A | ~B) & (~A | B)"
        ));
        UUID resultID = assertExecutionInitialized(result);
        ExecutionResultDto finalResult = waitForTermination(groverImpl.get(), resultID);

        Assertions.assertEquals(ExecutionResultStatus.FINISHED, finalResult.getStatus());
        System.out.println(finalResult.getResult());
        Assertions.assertTrue(finalResult.getResult().contains("counts"));
    }

}
