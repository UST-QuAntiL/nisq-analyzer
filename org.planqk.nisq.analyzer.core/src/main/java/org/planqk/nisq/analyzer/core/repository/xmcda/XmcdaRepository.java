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

package org.planqk.nisq.analyzer.core.repository.xmcda;

import java.util.List;
import java.util.Optional;

import org.planqk.nisq.analyzer.core.model.xmcda.CriterionValue;
import org.xmcda.v2.Criterion;

/**
 * Interface to access all XMCDA entities required for the prioritization feature of the NISQ Analyzer
 */
public interface XmcdaRepository {

    /**
     * Return all available criterion
     *
     * @return the list of available criterion
     */
    List<Criterion> findAll();

    /**
     * Return all criterion for which a corresponding value is available for the given MCDA method
     *
     * @param mcdaMethod the name of the MCDA method
     * @return the list of criterion
     */
    List<Criterion> findByMcdaMethod(String mcdaMethod);

    /**
     * Find a criterion by ID
     *
     * @param id the ID of the criterion
     * @return the optional containing the criterion or an empty optional if no criterion can be found
     */
    Optional<Criterion> findById(String id);

    /**
     * Find a criterion value by the ID of the criterion and the MCDA method this value belongs to
     *
     * @param criterionId the ID of the criterion
     * @param mcdaMethod the MCDA method to retrieve the criterion value for
     * @return the optional containing the criterion value or an empty optional if no criterion value can be found
     */
    Optional<CriterionValue> findByCriterionIdAndMethod(String criterionId, String mcdaMethod);

    /**
     * Update the stored criterion value object
     *
     * @param criterionValue the object containing the updated data
     */
    void updateCriterionValue(CriterionValue criterionValue);
}
