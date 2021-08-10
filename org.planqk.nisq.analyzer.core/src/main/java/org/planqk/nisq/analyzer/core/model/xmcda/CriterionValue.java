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

package org.planqk.nisq.analyzer.core.model.xmcda;

import lombok.Getter;
import lombok.Setter;

public class CriterionValue extends org.xmcda.v2.CriterionValue {

    @Getter
    @Setter
    private String mcdaMethod;

    public static CriterionValue fromXMCDA(org.xmcda.v2.CriterionValue xmcdaCriterion) {
        CriterionValue criterionValue = new CriterionValue();
        criterionValue.setCriterionID(xmcdaCriterion.getCriterionID());
        criterionValue.setCriteriaSet(xmcdaCriterion.getCriteriaSet());
        criterionValue.setCriteriaSetID(xmcdaCriterion.getCriteriaSetID());
        criterionValue.setDescription(xmcdaCriterion.getDescription());
        criterionValue.setId(xmcdaCriterion.getId());
        criterionValue.setName(xmcdaCriterion.getName());
        criterionValue.getValueOrValues().addAll(xmcdaCriterion.getValueOrValues());
        return criterionValue;
    }
}
