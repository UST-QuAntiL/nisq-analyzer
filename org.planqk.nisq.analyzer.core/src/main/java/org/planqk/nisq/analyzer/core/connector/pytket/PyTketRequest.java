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

package org.planqk.nisq.analyzer.core.connector.pytket;

import java.net.URL;
import java.util.Map;

import org.planqk.nisq.analyzer.core.model.ParameterValue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class PyTketRequest {

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
    private String provider;

    @Getter
    @Setter
    @JsonProperty(value = "transpiled-qasm")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String transpiled_qasm;

    @Getter
    @Setter
    @JsonProperty(value = "transpiled-quil")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String transpiled_quil;

    @Getter
    @Setter
    @JsonProperty("input-params")
    private Map<String, ParameterValue> input_params;

    @Getter
    @Setter
    @JsonProperty(value = "bearer-token")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bearerToken;

    @Getter
    @Setter
    @JsonProperty(value = "correlation-id")
    private String correlation_id;

    public PyTketRequest(URL impl_url, String impl_language, String qpu_name, String provider,
                         Map<String, ParameterValue> input_params, String bearerToken) {
        this.impl_url = impl_url;
        this.impl_language = impl_language;
        this.qpu_name = qpu_name;
        this.provider = provider;
        this.input_params = input_params;
        this.bearerToken = bearerToken;
    }

    public PyTketRequest(String impl_data, Map<String, ParameterValue> input_params, String impl_language,
                         String qpu_name, String provider) {
        this.impl_data = impl_data;
        this.impl_language = impl_language;
        this.qpu_name = qpu_name;
        this.provider = provider;
        this.input_params = input_params;
    }

    public PyTketRequest(String impl_data, String impl_language) {
        this.impl_data = impl_data;
        this.impl_language = impl_language;
    }

    public PyTketRequest(String qpu_name, Map<String, ParameterValue> input_params, String transpiled, String provider,
                         TranspiledLanguage language) {
        this.qpu_name = qpu_name;
        this.provider = provider;
        this.input_params = input_params;

        switch (language) {
            case Quil:
                this.transpiled_quil = transpiled;
                break;
            case OpenQASM:
                this.transpiled_qasm = transpiled;
                break;
            default:
                this.transpiled_qasm = null;
                this.transpiled_quil = null;
        }
    }

    public PyTketRequest(String qpu_name, Map<String, ParameterValue> input_params, String transpiled, String provider,
                         TranspiledLanguage language, String correlation_id, URL impl_url) {
        this.qpu_name = qpu_name;
        this.provider = provider;
        this.input_params = input_params;

        switch (language) {
            case Quil:
                this.transpiled_quil = transpiled;
                break;
            case OpenQASM:
                this.transpiled_qasm = transpiled;
                break;
            default:
                this.transpiled_qasm = null;
                this.transpiled_quil = null;
        }

        this.correlation_id = correlation_id;
        this.impl_url = impl_url;
    }

    public PyTketRequest(URL fileLocation, String language, Map<String, ParameterValue> parameters,
                         String bearerToken) {
        this.impl_url = fileLocation;
        this.impl_language = language;
        this.input_params = parameters;
        this.bearerToken = bearerToken;
    }

    public enum TranspiledLanguage {
        OpenQASM, Quil
    }
}
