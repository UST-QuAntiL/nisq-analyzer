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

package org.planqk.atlas.nisq.analyzer.knowledge.prolog;

import java.io.File;

/**
 * Constants for the prolog knowledge handling.
 */
public class Constants {

    // basic rule to check the executability of an implementation on the available QPUs
    public static final String QPU_RULE_NAME = "executableOnQpuRule";
    public static final String QPU_RULE_CONTENT = "executableOnQpu(RequiredQubits, CircuitDepth, Impl, Qpu) :- requiredSdk(Impl, ReqSdk), usedSdk(Qpu, ReqSdk), providesQubits(Qpu, ProvidedQubit), ProvidedQubit >= RequiredQubits, t1Time(Qpu,T1Time), maxGateTime(Qpu,GateTime), CircuitDepth =< T1Time/GateTime.";

    // path to store the files for the local knowledge base
    public static final String basePath = System.getProperty("java.io.tmpdir") + File.separator + "nisq-analyzer";
}
