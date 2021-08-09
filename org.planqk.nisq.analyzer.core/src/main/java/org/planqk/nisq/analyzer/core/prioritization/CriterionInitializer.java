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
import org.planqk.nisq.analyzer.core.repository.CriterionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.xmcda.v2.Criteria;
import org.xmcda.v2.XMCDA;
import org.xmcda.parsers.xml.xmcda_v2.XMCDAParser;
import org.xml.sax.SAXException;

@Service
public class CriterionInitializer {

    final private static Logger LOG = LoggerFactory.getLogger(CriterionInitializer.class);

    final private CriterionRepository criterionRepository;

    public CriterionInitializer(CriterionRepository criterionRepository) {
        this.criterionRepository = criterionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDbFromXmcdaXML() {

        try {
            LOG.debug("Initializing criterion database for prioritization from local criteria.xml file!");

            // read the file containing the criteria definition
            File criteriaFile = ResourceUtils.getFile("classpath:xmcda/criteria.xml");
            XMCDA xmcda = XMCDAParser.readXMCDA(criteriaFile);

            // retrieve the contained elements
            List<JAXBElement<?>> jaxbContents = xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters();

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
                    LOG.warn("Found criterion with the id '{}' in database and criteria.xml. Skipping to avoid overriding user changes!", criterion.getId());
                    continue;
                }

                criterionRepository.save(Criterion.fromXMCDA(criterion));
            }
            LOG.info("Found {} criterion in the repository after initialization!", criterionRepository.findAll().size());

            LOG.info("Successfully initialized database with definitions from resource folder!");
        } catch (IOException | SAXException | JAXBException e) {
            LOG.error("Error while ");
            e.printStackTrace();
        }
    }
}
