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

package org.planqk.nisq.analyzer.core.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class ParameterValue {

    final private static Logger LOG = LoggerFactory.getLogger(ParameterValue.class);

    @Getter
    @Setter
    DataType type;

    @Getter
    @Setter
    String rawValue;


    public static String convertToUntyped(ParameterValue parameter)
    {
        return parameter.rawValue;
    }

    public static Map<String,String> convertToUntyped(Map<String, ParameterValue> parameters)
    {
        Map<String,String> untypedParameters = new HashMap<>();

        // Convert all the entries to untyped versions
        for ( Map.Entry<String, ParameterValue> entry : parameters.entrySet())
        {
            untypedParameters.put(entry.getKey(), ParameterValue.convertToUntyped(entry.getValue()));
        }

        return untypedParameters;
    }

    public static ParameterValue inferTypedParameterValue(List<Parameter> parameters, String parameterName, String value)
    {
        try
        {
            DataType inferredType = parameters.stream().filter(p -> p.name.equals(parameterName)).findFirst().get().type;
            return new ParameterValue(inferredType, value);
        }
        catch (Exception e)
        {
            LOG.warn("Unable to infer type for parameter \"{}\". Continue with unknown type. This might influence the correct execution of the implementation.", parameterName);
            return new ParameterValue(DataType.Unknown, value);
        }
    }

    public static Map<String, ParameterValue> inferTypedParameterValue(List<Parameter> parameters, Map<String, String> values)
    {
        Map<String, ParameterValue> typedParameters = new HashMap<>();
        values.entrySet().stream().forEach( (entry) -> {
            typedParameters.put(entry.getKey(), inferTypedParameterValue(parameters, entry.getKey(), entry.getValue()));
        });
        return typedParameters;
    }
}
