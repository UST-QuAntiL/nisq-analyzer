/********************************************************************************
 * Copyright (c) 2022 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.connector.braket;

import java.net.URL;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.ParameterValue;

public class BraketRequest {

    @Getter
    @Setter
    @JsonProperty("impl-url")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private URL impl_url;

    @Getter
    @Setter
    @JsonProperty(value = "impl-language")
    private String impl_language;

    @Getter
    @Setter
    @JsonProperty(value = "impl-data")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String impl_data;

    @Getter
    @Setter
    @JsonProperty("qpu-name")
    private String qpu_name;

    @Getter
    @Setter
    @JsonProperty(value = "braket-ir")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String braket_ir;

    @Getter
    @Setter
    @JsonProperty("input-params")
    private Map<String, ParameterValue> input_params;

    @Getter
    @Setter
    @JsonProperty(value = "bearer-token")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bearerToken;

    public BraketRequest(URL impl_url, String impl_language, String qpu_name, Map<String, ParameterValue> input_params, String bearerToken) {
        this.impl_url = impl_url;
        this.impl_language = impl_language;
        this.qpu_name = qpu_name;
        this.input_params = input_params;
        this.bearerToken = bearerToken;
    }

    public BraketRequest(String impl_language, String impl_data, String qpu_name, Map<String, ParameterValue> input_params) {
        this.impl_data = impl_data;
        this.impl_language = impl_language;
        this.qpu_name = qpu_name;
        this.input_params = input_params;
    }

    public BraketRequest(String braket_ir, String qpu_name, Map<String, ParameterValue> input_params) {
        this.braket_ir = braket_ir;
        this.qpu_name = qpu_name;
        this.input_params = input_params;
        this.impl_language = Constants.BRAKET_IR;
    }
}

