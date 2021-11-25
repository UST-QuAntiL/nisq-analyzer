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

package org.planqk.nisq.analyzer.core.web.dtos.entities;

import org.planqk.nisq.analyzer.core.model.CompilationResult;
import org.springframework.hateoas.server.core.Relation;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Relation(itemRelation = "compilerAnalysisResult", collectionRelation = "compilerAnalysisResults")
@EqualsAndHashCode(callSuper = false)
@Data
public class CompilerAnalysisResultDto extends CircuitResultDto {

    String initialCircuit;

    String circuitName;

    String transpiledCircuit;

    public static final class Converter {

        public static CompilerAnalysisResultDto convert(final CompilationResult object) {
            CompilerAnalysisResultDto dto = new CompilerAnalysisResultDto();
            dto.setId(object.getId());
            dto.setProvider(object.getProvider());
            dto.setQpu(object.getQpu());
            dto.setCompiler(object.getCompiler());
            dto.setAnalyzedDepth(object.getAnalyzedDepth());
            dto.setAnalyzedWidth(object.getAnalyzedWidth());
            dto.setAnalyzedTotalNumberOfOperations(object.getAnalyzedTotalNumberOfOperations());
            dto.setAnalyzedNumberOfSingleQubitGates(object.getAnalyzedNumberOfSingleQubitGates());
            dto.setAnalyzedNumberOfMeasurementOperations(object.getAnalyzedNumberOfMeasurementOperations());
            dto.setAnalyzedNumberOfMultiQubitGates(object.getAnalyzedNumberOfMultiQubitGates());
            dto.setAnalyzedMultiQubitGateDepth(object.getAnalyzedMultiQubitGateDepth());
            dto.setCircuitName(object.getCircuitName());
            dto.setInitialCircuit(object.getInitialCircuit());
            dto.setTranspiledCircuit(object.getTranspiledCircuit());
            dto.setAvgMultiQubitGateError(object.getAvgMultiQubitGateError());
            dto.setAvgMultiQubitGateTime(object.getAvgMultiQubitGateTime());
            dto.setAvgSingleQubitGateError(object.getAvgSingleQubitGateError());
            dto.setAvgSingleQubitGateTime(object.getAvgSingleQubitGateTime());
            dto.setAvgReadoutError(object.getAvgReadoutError());
            dto.setMaxGateTime(object.getMaxGateTime());
            dto.setT1(object.getT1());
            dto.setT2(object.getT2());
            dto.setNumberOfQubits(object.getQubitCount());
            dto.setSimulator(object.isSimulator());
            dto.setTime(object.getTime());
            return dto;
        }
    }
}
