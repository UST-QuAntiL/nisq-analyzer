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

import org.hibernate.annotations.GenericGenerator;

import lombok.Getter;
import lombok.Setter;

@Entity
public class Criterion extends org.xmcda.v2.Criterion {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "uuid", updatable = false, nullable = false)
    private UUID uuid;

    @Getter
    @Setter
    private String id;

    public static Criterion fromXMCDA(org.xmcda.v2.Criterion xmcdaCriterion) {
        Criterion criterion = new Criterion();
        criterion.setId(xmcdaCriterion.getId());
        criterion.setDescription(xmcdaCriterion.getDescription());
        criterion.setName(xmcdaCriterion.getName());
        criterion.setMcdaConcept(xmcdaCriterion.getMcdaConcept());
        return criterion;
    }

    public static org.xmcda.v2.Criterion toXMCDA(Criterion criterion) {
        org.xmcda.v2.Criterion xmcdaCriterion = new org.xmcda.v2.Criterion();
        xmcdaCriterion.setId(criterion.getId());
        xmcdaCriterion.setDescription(criterion.getDescription());
        xmcdaCriterion.setName(criterion.getName());
        xmcdaCriterion.setMcdaConcept(criterion.getMcdaConcept());
        return xmcdaCriterion;
    }
}
