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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * Evaluate the given prolog width rule with the given set of parameters to estimate the circuit width
     *
     * @param widthRule the prolog width rule to evaluate to get the estimated circuit width
     * @param params    the set of parameters to use for the evaluation
     * @return the estimated circuit width, or zero if an error occurs
     */
    public int checkWidth(String widthRule, Map<String, String> params) {
        // TODO
        return 0;
    }

    /**
     * Evaluate the given prolog depth rule with the given set of parameters to estimate the circuit depth
     *
     * @param depthRule the prolog depth rule to evaluate to get the estimated circuit depth
     * @param params    the set of parameters to use for the evaluation
     * @return the estimated circuit depth, or zero if an error occurs
     */
    public int checkDepth(String depthRule, Map<String, String> params) {
        // TODO
        return 0;
    }

    /**
     * Evaluate the given prolog selection rule with the given set of parameters
     *
     * @param selectionRule the prolog selection rule to evaluate to check the executability
     * @param params        the set of parameters to use for the evaluation
     * @return the evaluation result of the prolog rule
     */
    public boolean checkExecutability(String selectionRule, Map<String, String> params) {
        // retrieve signature of the defined selection rule
        String signature = PrologUtility.getSignatureOfRule(selectionRule);
        String[] signatureParts = signature.split("\\(");

        // rule is invalid as it does not contain brackets for the parameters
        if (signatureParts.length < 2) {
            LOG.error("Signature of selection rule is invalid: {}", signature);
            return false;
        }

        String ruleName = signatureParts[0];
        String parameterPart = signatureParts[1];

        // replace the variables in the signature with the given parameters
        for (String variable : PrologUtility.getVariablesForPrologRule(selectionRule)) {
            if (!params.containsKey(variable)) {
                LOG.error("Given parameter set to check executability does not contain required parameter: {}", variable);
                return false;
            }

            // FIXME: avoid replacing parts of another variable where the name contains the searched variable, e.g., search for variable 'A' and replace part of variable 'AB' by accident
            parameterPart = parameterPart.replaceFirst(variable, params.get(variable));
        }

        // add point to instruct prolog to evaluate the rule
        String query = ruleName + "(" + parameterPart + ".";

        // evaluate the rule in the knowledge base
        boolean evaluationResult = prologKnowledgeBaseHandler.hasSolution(query);
        LOG.debug("Evaluated selection rule '{}' with result: {}", query, evaluationResult);
        return evaluationResult;
    }

    /**
     * Check the prolog knowledge base for QPUs that can handle the given implementation and return them
     *
     * @param implementationId the id of the implementation for which
     * @param requiredQubits   the number of qubits that are required for the execution
     * @param circuitDepth     the depth of the circuit representation of the implementation
     * @return a list with an Id for each QPU that can execute the given implementation
     */
    public List<Long> getSuitableQpus(Long implementationId, int requiredQubits, int circuitDepth) {
        // check if file with required rule exists and create otherwise
        if (!prologKnowledgeBaseHandler.doesPrologFileExist(Constants.QPU_RULE_NAME)) {
            try {
                prologKnowledgeBaseHandler.persistPrologFile(Constants.QPU_RULE_CONTENT, Constants.QPU_RULE_NAME);
            } catch (IOException e) {
                LOG.error("Unable to persist prolog file with QPU selection rule. Unable to determine suitable QPUs!");
                return new ArrayList<>();
            }
        }
        prologKnowledgeBaseHandler.activatePrologFile(Constants.QPU_RULE_NAME);

        List<Long> suitableQPUs = new ArrayList<>();

        // determine the suited QPUs for the implementation and the width/depth through the Prolog knowledge base
        String qpuVariable = "Qpu";
        String query = "executableOnQpu(" + requiredQubits + "," + circuitDepth + "," + implementationId + "," + qpuVariable + ").";
        LOG.debug("Executing the following query to determine the suitable QPUs: {}", query);
        Map<String, Term>[] solutions = getSolutions(query);

        // parse Ids of suitable QPUs from response
        if (Objects.nonNull(solutions)) {
            LOG.debug("Retrieved {} possible qpu candidates for the query.", solutions.length);
            for (Map<String, Term> solution : solutions) {
                if (Objects.nonNull(solution.get(qpuVariable))) {
                    LOG.debug("Found solution: {}", solution.get(qpuVariable));
                    suitableQPUs.add(solution.get(qpuVariable).longValue());
                }
            }
        }

        return suitableQPUs;
    }
}
