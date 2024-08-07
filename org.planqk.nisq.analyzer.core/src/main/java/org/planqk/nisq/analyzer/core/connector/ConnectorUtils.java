/*******************************************************************************
 * Copyright (c) 2024 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.connector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;

public class ConnectorUtils {

    public static void calculateHistogramIntersection(ExecutionResult executionResult,
                                                      ExecutionResultRepository resultRepository,
                                                      QpuSelectionResultRepository qpuSelectionResultRepository,
                                                      ExecutionRequestResult result, QpuSelectionResult qResult,
                                                      String simulator) {
        if (!qResult.getQpu().contains(simulator)) {
            List<QpuSelectionResult> jobResults =
                qpuSelectionResultRepository.findAllByQpuSelectionJobId(qResult.getQpuSelectionJobId());
            // get qpuSelectionResult of simulator if available
            QpuSelectionResult simulatorQpuSelectionResult =
                jobResults.stream().filter(jobResult -> jobResult.getQpu().contains(simulator)).findFirst()
                    .orElse(null);
            if (Objects.nonNull(simulatorQpuSelectionResult)) {
                //check if qpu-selection result of simulator was already executed otherwise
                // wait max 1 minute
                int iterator = 60;
                while (iterator > 0) {
                    try {
                        Thread.sleep(1000);
                        ExecutionResult simulatorExecutionResult = resultRepository.findAll().stream()
                            .filter(exResults -> Objects.nonNull(exResults.getQpuSelectionResult())).filter(
                                exeResult -> exeResult.getQpuSelectionResult().getId()
                                    .equals(simulatorQpuSelectionResult.getId())).findFirst().orElse(null);

                        // as soon as execution result of simulator is returned calculate
                        // histogram intersection
                        if (Objects.nonNull(simulatorExecutionResult)) {
                            // convert stored execution result of simulator to Map
                            String simulatorExecutionResultString = simulatorExecutionResult.getResult();
                            Map<String, Integer> simulatorCountsOfResults = new HashMap<>();
                            String rawData = simulatorExecutionResultString.replaceAll("[\\{\\}\\s+]", "");
                            String[] instances = rawData.split(",");
                            for (String instance : instances) {
                                String[] resultsData = instance.split("=");
                                String measurementResult = resultsData[0].trim();
                                int counts = Integer.parseInt(resultsData[1].trim());
                                simulatorCountsOfResults.put(measurementResult, counts);
                            }

                            Map<String, Integer> qpuExecutionResult = result.getResult();
                            // histogram intersection calculation
                            double intersection = 0;
                            for (String qpuKey : qpuExecutionResult.keySet()) {
                                if (!simulatorCountsOfResults.containsKey(qpuKey)) {
                                    simulatorCountsOfResults.put(qpuKey, 0);
                                }
                            }
                            for (String simulatorKey : simulatorCountsOfResults.keySet()) {
                                if (!qpuExecutionResult.containsKey(simulatorKey)) {
                                    qpuExecutionResult.put(simulatorKey, 0);
                                }
                                intersection = intersection + Math.min(simulatorCountsOfResults.get(simulatorKey),
                                    qpuExecutionResult.get(simulatorKey));
                            }
                            if (intersection > 0) {
                                executionResult.setHistogramIntersectionValue(
                                    intersection / simulatorExecutionResult.getShots());
                            }
                            break;
                        }
                        iterator--;
                    } catch (InterruptedException e) {
                        // pass
                    }
                }
            }
        } else {
            executionResult.setHistogramIntersectionValue(1);
        }
    }
}
