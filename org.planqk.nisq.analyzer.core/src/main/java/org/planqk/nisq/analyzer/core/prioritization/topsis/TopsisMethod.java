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

package org.planqk.nisq.analyzer.core.prioritization.topsis;

import java.util.UUID;

import org.planqk.nisq.analyzer.core.model.McdaJob;
import org.planqk.nisq.analyzer.core.prioritization.McdaMethod;
import org.springframework.stereotype.Service;

/**
 * Service implementing the TOPSIS method to prioritize analysis results of the NISQ Analyzer.
 */
@Service
public class TopsisMethod implements McdaMethod {

    @Override
    public String getName() {
        return "topsis";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }

    @Override
    public void executeMcdaMethod(McdaJob mcdaJob, UUID jobId) {
        // TODO
    }
}
