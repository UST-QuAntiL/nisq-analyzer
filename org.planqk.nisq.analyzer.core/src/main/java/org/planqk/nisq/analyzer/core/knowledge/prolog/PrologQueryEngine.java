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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jpl7.PrologException;
import org.jpl7.Query;
import org.jpl7.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Class to execute different kinds of required prolog queries.
 */
@Service
public class PrologQueryEngine {

    final private static Logger LOG = LoggerFactory.getLogger(PrologQueryEngine.class);

    final private PrologKnowledgeBaseHandler prologKnowledgeBaseHandler;

    public PrologQueryEngine(PrologKnowledgeBaseHandler prologKnowledgeBaseHandler) {
        this.prologKnowledgeBaseHandler = prologKnowledgeBaseHandler;
    }

    /**
     * Execute a prolog query with variables and return all possible solutions
     *
     * @param queryContent the content of the query
     * @return an array with a map for each solution containing the values of the query variables, or <code>null</code>
     * if an error occurred
     */
    private static Map<String, Term>[] getSolutions(String queryContent) {
        LOG.debug("Executing query with the following content to retrieve solutions: {}", queryContent);
        try {
            Map<String, Term>[] solutions = Query.allSolutions(queryContent);
            LOG.debug("Number of solutions: {}", solutions.length);
            return solutions;
        } catch (PrologException e) {
            LOG.warn("Prolog error while executing query. Procedure may not exist in knowledge base...");
            return null;
        }
    }

    /**
     * Evaluate the given prolog selection rule with the given set of parameters
     *
     * @param selectionRule the prolog selection rule to evaluate to check the executability
     * @param params        the set of parameters to use for the evaluation
     * @return the evaluation result of the prolog rule
     */

    public boolean checkExecutability(String selectionRule, Map<String, String> params) {
        String query = assembleQueryForRule(selectionRule, params, false);
        if (Objects.isNull(query)) {
            LOG.error("Unable to evaluate selection rule!");
            return false;
        }

        // evaluate the rule in the knowledge base
        boolean evaluationResult = prologKnowledgeBaseHandler.hasSolution(query);
        LOG.debug("Evaluated selection rule '{}' with result: {}", query, evaluationResult);
        return evaluationResult;
    }

    /**
     * Check the prolog knowledge base if the QPU can handle the given implementation
     *
     * @param implementationId the id of the implementation for which
     * @param qpuId the id of the qpu
     * @param requiredQubits   the number of qubits that are required for the execution
     * @param circuitDepth     the depth of the circuit representation of the implementation
     * @return
     */
    public boolean isQpuSuitable(UUID implementationId, UUID qpuId, int requiredQubits, int circuitDepth) {

        // check if file with required rule exists and create otherwise
        if (!prologKnowledgeBaseHandler.doesPrologFileExist(Constants.QPU_RULE_NAME)) {
            try {
                prologKnowledgeBaseHandler.persistPrologFile(Constants.QPU_RULE_CONTENT, Constants.QPU_RULE_NAME);
            } catch (IOException e) {
                LOG.error("Unable to persist prolog file with QPU selection rule. Unable to determine suitable QPUs!");
                return false;
            }
        }
        prologKnowledgeBaseHandler.activatePrologFile(Constants.QPU_RULE_NAME);


        // determine the suited QPU for the implementation and the width/depth through the Prolog knowledge base
        String query = "executableOnQpu(" + requiredQubits + "," + circuitDepth + ",'" + implementationId + "','" + qpuId + "').";
        boolean evaluationResult = prologKnowledgeBaseHandler.hasSolution(query);
        LOG.debug("Executing the following query to determine if the QPU is suitable: {} with result {}.", query, evaluationResult);
        return evaluationResult;
    }

    /**
     * Check the prolog knowledge base for QPUs that can handle the given implementation and return them
     *
     * @param implementationId the id of the implementation for which
     * @return a list with an Id for each QPU that can execute the given implementation
     */
    public List<UUID> getSuitableQpus(UUID implementationId) {
        // check if file with required rule exists and create otherwise
        if (!prologKnowledgeBaseHandler.doesPrologFileExist(Constants.QPU_TRANSP_RULE_NAME)) {
            try {
                prologKnowledgeBaseHandler.persistPrologFile(Constants.QPU_TRANSP_RULE_CONTENT, Constants.QPU_TRANSP_RULE_NAME);
            } catch (IOException e) {
                LOG.error("Unable to persist prolog file with QPU selection rule. Unable to determine suitable QPUs!");
                return new ArrayList<>();
            }
        }
        prologKnowledgeBaseHandler.activatePrologFile(Constants.QPU_TRANSP_RULE_NAME);

        List<UUID> suitableQPUs = new ArrayList<>();

        // determine the suited QPUs for the implementation and the width/depth through the Prolog knowledge base
        String qpuVariable = "Qpu";
        String query = "transpilableOnQpu('" + implementationId + "'," + qpuVariable + ").";
        LOG.debug("Executing the following query to determine the suitable QPUs: {}", query);
        Map<String, Term>[] solutions = getSolutions(query);

        // parse Ids of suitable QPUs from response
        if (Objects.nonNull(solutions)) {
            LOG.debug("Retrieved {} possible qpu candidates for the query.", solutions.length);
            for (Map<String, Term> solution : solutions) {
                if (Objects.nonNull(solution.get(qpuVariable))) {
                    LOG.debug("Found solution: {}", solution.get(qpuVariable).name());
                    try {
                        suitableQPUs.add(UUID.fromString(solution.get(qpuVariable).name()));
                    } catch (IllegalArgumentException e) {
                        LOG.error("Unable to parse result to UUID!");
                    }
                }
            }
        }

        return suitableQPUs;
    }

    /**
     * Assemble a query to evaluate the given rule with the given set of parameters
     *
     * @param rule      the rule to evaluate
     * @param params    the set of parameters to insert for the evaluation
     * @param skipFirst <code>true</code> if the first variable in the rule should not be replaced, <code>false</code>
     *                  otherwise
     * @return the query or <code>null</code> if the rule or parameters are invalid
     */
    private String assembleQueryForRule(String rule, Map<String, String> params, boolean skipFirst) {
        // retrieve signature of the defined selection rule
        String signature = PrologUtility.getSignatureOfRule(rule);
        String[] signatureParts = signature.split("\\(");

        // rule is invalid as it does not contain brackets for the parameters
        if (signatureParts.length < 2) {
            LOG.error("Signature of rule is invalid: {}", signature);
            return null;
        }

        String ruleName = signatureParts[0];
        String parameterPart = signatureParts[1];

        List<String> variables = PrologUtility.getVariablesForPrologRule(rule);
        for (int i = 0; i < variables.size(); i++) {
            // do not replace the first variable if it is used for the evaluation of the rule
            if (skipFirst && i == 0) {
                continue;
            }

            // replace the variable in the signature if parameter is available
            String variable = variables.get(i);
            if (!params.containsKey(variable)) {
                LOG.error("Given parameter set to evaluate rule does not contain required parameter: {}", variable);
                return null;
            }

            // FIXME: avoid replacing parts of another variable where the name contains the searched variable, e.g., search for variable 'A' and replace part of variable 'AB' by accident
            parameterPart = parameterPart.replaceFirst(variable, params.get(variable));
        }

        // add point to instruct prolog to evaluate the rule
        return ruleName + "(" + parameterPart + ".";
    }
}