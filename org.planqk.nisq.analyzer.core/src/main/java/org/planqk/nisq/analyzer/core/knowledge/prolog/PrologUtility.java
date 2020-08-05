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

package org.planqk.nisq.analyzer.core.knowledge.prolog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.model.DataType;
import org.planqk.nisq.analyzer.core.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for the handling of prolog facts and rules
 */
public class PrologUtility {

    final private static Logger LOG = LoggerFactory.getLogger(PrologUtility.class);

    /**
     * Get the set of parameters that are required to evaluate the given rule
     *
     * @param rule      the rule to retrieve the parameters from
     * @param skipFirst <code>true</code> if the first variable is used for evaluating the rule (e.g. width rule) and
     *                  is thus no parameter, <code>false</code> otherwise
     * @return the set of required parameters to evaluate the rule
     */
    public static Set<Parameter> getParametersForRule(String rule, boolean skipFirst) {
        if (Objects.isNull(rule)) {
            return new HashSet<>();
        }

        Set<Parameter> result = new HashSet<>();
        List<String> variables = getVariablesForPrologRule(rule);
        for (int i = 0; i < variables.size(); i++) {
            if (skipFirst && i == 0) {
                continue;
            }
            result.add(new Parameter(variables.get(i), DataType.String, null, "Parameter of rule."));
        }
        return result;
    }

    /**
     * Check if the given parameter of a prolog rule is a variable
     *
     * @param parameter the parameter to check
     * @return <code>true</code> if the parameter is a variable, <code>false</code> otherwise
     */
    public static boolean isVariable(String parameter) {
        LOG.debug("Checking if param {} is variable.", parameter);

        // variables have to start with an uppercase letter or an underscore
        return Character.isUpperCase(parameter.charAt(0)) || parameter.startsWith("_");
    }

    /**
     * Get the ordered list of variables of the given rule
     *
     * @param rule the rule to retrieve the parameters from
     * @return the ordered list of parameters
     */
    public static List<String> getVariablesForPrologRule(String rule) {
        LOG.debug("Getting parameters for rule: {}", rule);

        // get String part between the brackets
        String[] ruleParts = rule.split("\\(");

        // rule is invalid as it does not contain brackets for the parameters
        if (ruleParts.length < 2) {
            return new ArrayList<>();
        }

        // get the set of parameters
        String parametersPart = ruleParts[1].split("\\)")[0];
        String[] params = parametersPart.split(",");
        LOG.debug("Rule contains {} parameters.", params.length);

        return Arrays.stream(params)
                .map(parameter -> parameter.replaceAll("\\s", ""))
                .filter(PrologUtility::isVariable)
                .collect(Collectors.toList());
    }

    /**
     * Get the number of parameters that are used for a given rule.
     *
     * @param rule the rule to get the parameter count from
     * @return the number of available parameters
     */
    public static int getNumberOfParameters(String rule) {
        // get String part between the brackets
        String[] ruleParts = rule.split("\\(");

        // rule is invalid as it does not contain brackets for the parameters
        if (ruleParts.length < 2) {
            return 0;
        }

        String parametersPart = rule.split("\\)")[0];
        return parametersPart.split(",").length;
    }

    /**
     * Retrieve the signature of the given prolog rule
     *
     * @param rule the rule to get the signature from
     * @return the signature (left part) of the rule
     */
    public static String getSignatureOfRule(String rule) {
        return rule.split(":")[0].replaceAll("\\s", "");
    }
}
