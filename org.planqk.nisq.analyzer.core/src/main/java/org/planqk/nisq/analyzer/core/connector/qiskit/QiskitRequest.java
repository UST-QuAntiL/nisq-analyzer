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

package org.planqk.nisq.analyzer.core.connector.qiskit;

import java.util.Map;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.planqk.nisq.analyzer.core.model.ParameterValue;

public class QiskitRequest {

    @Getter
    @Setter
    @JsonProperty("impl-url")
    private URL impl_url;

    @Getter
    @Setter
    @JsonProperty("qpu-name")
    private String qpu_name;

    @Getter
    @Setter
    @JsonProperty("input-params")
    private Map<String, ParameterValue> input_params;

    public QiskitRequest(URL impl_url, String qpu_name, Map<String,ParameterValue> input_params)
    {
        this.impl_url = impl_url;
        this.qpu_name = qpu_name;
        this.input_params = input_params;
    }
}
