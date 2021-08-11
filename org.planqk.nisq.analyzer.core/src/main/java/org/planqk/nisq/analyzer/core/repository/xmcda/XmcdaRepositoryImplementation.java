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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import javax.xml.bind.JAXBException;

import org.planqk.nisq.analyzer.core.model.xmcda.CriterionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;
import org.xmcda.v2.Criterion;
import org.xml.sax.SAXException;

/**
 * XmcdaRepository implementation loading the XMCDA entities from local files
 * <p>
 * FIXME: move to database implementation
 */
@Service
public class XmcdaRepositoryImplementation implements XmcdaRepository {

    final private static Logger LOG = LoggerFactory.getLogger(XmcdaRepositoryImplementation.class);

    final private ResourceLoader resourceLoader;

    final private List<Criterion> criterionList = new ArrayList<>();

    final private List<CriterionValue> criterionValueList = new ArrayList<>();

    public XmcdaRepositoryImplementation(ResourceLoader resourceLoader, CriterionInitializer criterionInitializer) {
        initializeRepository(criterionInitializer);
        this.resourceLoader = resourceLoader;
    }

    private void initializeRepository(CriterionInitializer criterionInitializer) {
        LOG.debug("Initializing databases for prioritization with definitions from resource folder!");

        try {
            criterionList.addAll(criterionInitializer.initializeCriterion());
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources("classpath:xmcda/initial-weights-*");
            LOG.debug(String.valueOf(resources.length));
            for (Resource mcdaMethod : resources) {
                criterionValueList.addAll(criterionInitializer.initializeWeightsForCriterion(mcdaMethod.getFilename()));
            }

            LOG.info("Successfully initialized repository with definitions from resource folder!");
        } catch (IOException | JAXBException | SAXException e) {
            LOG.error("Error while initializing repository from local XML files: {}", e.getLocalizedMessage());
        }
    }

    @Override
    public List<Criterion> findAll() {
        return criterionList;
    }

    @Override
    public List<Criterion> findByMcdaMethod(String mcdaMethod) {
        return criterionList.stream()
                .filter(criterion -> criterionValueList.stream().anyMatch(criterionValue -> criterionValue.getMcdaMethod().equals(mcdaMethod) &&
                        criterionValue.getCriterionID().equals(criterion.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Criterion> findById(String id) {
        return criterionList.stream()
                .filter(criterion -> criterion.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<CriterionValue> findByCriterionIdAndMethod(String criterionId, String mcdaMethod) {
        return criterionValueList.stream()
                .filter(criterionValue -> criterionValue.getCriterionID().equals(criterionId))
                .filter(criterionValue -> criterionValue.getMcdaMethod().equals(mcdaMethod))
                .findFirst();
    }

    @Transactional
    @Override
    public void updateCriterionValue(CriterionValue criterionValue) {
        Optional<CriterionValue> oldValue = findByCriterionIdAndMethod(criterionValue.getCriterionID(), criterionValue.getMcdaMethod());
        if (!oldValue.isPresent()) {
            LOG.error("Unable to find criterion value for criterion ID {} and MCDA method {}. Skipping update!", criterionValue.getCriterionID(),
                    criterionValue.getMcdaMethod());
            return;
        }

        criterionValueList.remove(oldValue.get());
        criterionValueList.add(criterionValue);
    }
}
