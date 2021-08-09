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

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

import lombok.Getter;
import lombok.Setter;

@Entity
public class CriterionValue extends org.xmcda.v2.CriterionValue {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "uuid", updatable = false, nullable = false)
    private UUID uuid;

    @Getter
    @Setter
    @ManyToOne
    private Criterion criterion;

    @Getter
    @Setter
    private String mcdaMethod;

    public static CriterionValue fromXMCDA(org.xmcda.v2.CriterionValue xmcdaCriterionValue) {
        CriterionValue criterionValue = new CriterionValue();
        criterionValue.setId(xmcdaCriterionValue.getId());
        criterionValue.setDescription(xmcdaCriterionValue.getDescription());
        criterionValue.setName(xmcdaCriterionValue.getName());
        criterionValue.setMcdaConcept(xmcdaCriterionValue.getMcdaConcept());
        criterionValue.setCriterionID(xmcdaCriterionValue.getCriterionID());
        criterionValue.setCriteriaSet(xmcdaCriterionValue.getCriteriaSet());
        criterionValue.setCriteriaSetID(xmcdaCriterionValue.getCriteriaSetID());
        criterionValue.getValueOrValues().addAll(xmcdaCriterionValue.getValueOrValues());
        return criterionValue;
    }

    public static org.xmcda.v2.CriterionValue toXMCDA(CriterionValue criterionValue) {
        org.xmcda.v2.CriterionValue xmcdaCriterionValue = new org.xmcda.v2.CriterionValue();
        xmcdaCriterionValue.setId(criterionValue.getId());
        xmcdaCriterionValue.setDescription(criterionValue.getDescription());
        xmcdaCriterionValue.setName(criterionValue.getName());
        xmcdaCriterionValue.setMcdaConcept(criterionValue.getMcdaConcept());
        xmcdaCriterionValue.setCriterionID(criterionValue.getCriterionID());
        xmcdaCriterionValue.setCriteriaSet(criterionValue.getCriteriaSet());
        xmcdaCriterionValue.setCriteriaSetID(criterionValue.getCriteriaSetID());
        xmcdaCriterionValue.getValueOrValues().addAll(criterionValue.getValueOrValues());
        return xmcdaCriterionValue;
    }
}
