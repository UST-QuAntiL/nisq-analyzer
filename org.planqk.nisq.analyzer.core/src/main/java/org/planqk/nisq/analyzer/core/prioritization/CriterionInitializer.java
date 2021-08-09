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

package org.planqk.nisq.analyzer.core.prioritization;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.planqk.nisq.analyzer.core.model.xmcda.Criterion;
import org.planqk.nisq.analyzer.core.model.xmcda.CriterionValue;
import org.planqk.nisq.analyzer.core.repository.xmcda.CriterionRepository;
import org.planqk.nisq.analyzer.core.repository.xmcda.CriterionValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.xmcda.parsers.xml.xmcda_v2.XMCDAParser;
import org.xmcda.v2.Criteria;
import org.xmcda.v2.CriteriaValues;
import org.xmcda.v2.XMCDA;
import org.xml.sax.SAXException;

@Service
public class CriterionInitializer {

    final private static Logger LOG = LoggerFactory.getLogger(CriterionInitializer.class);

    final private CriterionRepository criterionRepository;

    final private CriterionValueRepository criterionValueRepository;

    final private List<McdaMethod> mcdaMethods;

    public CriterionInitializer(CriterionRepository criterionRepository,
                                CriterionValueRepository criterionValueRepository,
                                List<McdaMethod> mcdaMethods) {
        this.criterionRepository = criterionRepository;
        this.criterionValueRepository = criterionValueRepository;
        this.mcdaMethods = mcdaMethods;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDbFromXmcdaXML() {

        try {
            LOG.debug("Initializing databases for prioritization with definitions from resource folder!");

            initializeCriterion();
            for (McdaMethod mcdaMethod : mcdaMethods) {
                initializeWeightsForCriterion(mcdaMethod.getName());
            }

            LOG.info("Successfully initialized database with definitions from resource folder!");
        } catch (IOException | SAXException | JAXBException e) {
            LOG.error("Error while initializing database from local XML files: {}", e.getLocalizedMessage());
        }
    }

    /**
     * Initialize the criterion database with the data from the criteria.xml
     */
    private void initializeCriterion() throws IOException, JAXBException, SAXException {
        LOG.debug("Initializing criterion database for prioritization from local criteria.xml file!");

        // retrieve the JAXB elements from the criteria.xml file
        List<JAXBElement<?>> jaxbContents = getJaxbContentsFromXmcdaFile("criteria.xml");

        // only one element is allowed to be in the file
        if (jaxbContents.size() != 1) {
            LOG.error("criteria.xml must contain exactly one element under the xmcda tag and contains: {}", jaxbContents.size());
            return;
        }

        // parse criteria object from JAXB element
        Criteria criteria = (Criteria) jaxbContents.get(0).getValue();

        LOG.info("Found {} criterion in resource file!", criteria.getCriterion().size());
        for (org.xmcda.v2.Criterion criterion : criteria.getCriterion()) {
            if (!criterionRepository.findById(criterion.getId()).isEmpty()) {
                LOG.warn("Found criterion with the id '{}' in database and criteria.xml. Skipping to avoid overriding user changes!",
                        criterion.getId());
                continue;
            }

            criterionRepository.save(Criterion.fromXMCDA(criterion));
        }
        LOG.debug("Found {} criterion in the repository after initialization!", criterionRepository.findAll().size());
    }

    /**
     * Initialize the weights for the different criterion using the default values provided by weights.xml if not already defined in the database
     *
     * @param methodName the name of the MCDA method to load the initial weights for
     */
    private void initializeWeightsForCriterion(String methodName) throws JAXBException, IOException, SAXException {
        LOG.debug("Initializing criterion values database for prioritization from local initial-weights-{}.xml file!", methodName);

        // retrieve the JAXB elements from the xml file
        List<JAXBElement<?>> jaxbContents = getJaxbContentsFromXmcdaFile("initial-weights-" + methodName + ".xml");

        // only one element is allowed to be in the file
        if (jaxbContents.size() != 1) {
            LOG.error("initial-weights-{}.xml must contain exactly one element under the xmcda tag and contains: {}", methodName, jaxbContents.size());
            return;
        }

        // parse criteria values object from JAXB element
        CriteriaValues criteriaValues = (CriteriaValues) jaxbContents.get(0).getValue();

        LOG.info("Found {} criterion values in resource file!", criteriaValues.getCriterionValue().size());
        for (org.xmcda.v2.CriterionValue criterionValue : criteriaValues.getCriterionValue()) {

            // get criterion to which the value should be added
            List<Criterion> criterionList = criterionRepository.findById(criterionValue.getCriterionID());
            if (criterionList.isEmpty()) {
                LOG.warn("Unable to find criterion with id '{}' in database to add corresponding value!", criterionValue.getCriterionID());
                continue;
            }

            // check if corresponding value is already defined
            Criterion criterion = criterionList.get(0);
            if (criterion.getCriterionValues().stream().anyMatch(value -> value.getMcdaMethod().equals(methodName))) {
                LOG.warn("Criterion with ID '{}' has already a criterion value defined for MCDA method {}. Skipping to avoid overriding user changes!", criterion.getId(), methodName);
                continue;
            }

            // add criterion value to criteria and update repository
            CriterionValue internalCriterionValue = CriterionValue.fromXMCDA(criterionValue);
            internalCriterionValue.setCriterion(criterion);
            internalCriterionValue.setMcdaMethod(methodName);
            internalCriterionValue = criterionValueRepository.save(internalCriterionValue);
            criterion.getCriterionValues().add(internalCriterionValue);
            criterionRepository.save(criterion);
        }
        LOG.debug("Found {} criterion values in the repository after initialization!", criterionValueRepository.findAll().size());
    }

    private List<JAXBElement<?>> getJaxbContentsFromXmcdaFile(String fileName) throws JAXBException, IOException, SAXException {
        // read the file containing the criteria definition
        File criteriaFile = ResourceUtils.getFile("classpath:xmcda/" + fileName);
        XMCDA xmcda = XMCDAParser.readXMCDA(criteriaFile);

        // retrieve the contained elements
        return xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters();
    }
}
