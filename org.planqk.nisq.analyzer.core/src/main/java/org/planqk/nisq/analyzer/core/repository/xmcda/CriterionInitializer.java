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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.planqk.nisq.analyzer.core.model.xmcda.CriterionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.xmcda.parsers.xml.xmcda_v2.XMCDAParser;
import org.xmcda.v2.Criteria;
import org.xmcda.v2.CriteriaValues;
import org.xmcda.v2.Criterion;
import org.xmcda.v2.XMCDA;
import org.xml.sax.SAXException;

@Service
public class CriterionInitializer {

    final private static Logger LOG = LoggerFactory.getLogger(CriterionInitializer.class);

    /**
     * Return the XMCDA criterion from the criteria.xml file
     */
    public List<Criterion> initializeCriterion() throws IOException, JAXBException, SAXException {
        LOG.debug("Initializing criterion database for prioritization from local criteria.xml file!");

        // retrieve the JAXB elements from the criteria.xml file
        List<JAXBElement<?>> jaxbContents = getJaxbContentsFromXmcdaFile("criteria.xml");

        // only one element is allowed to be in the file
        if (jaxbContents.size() != 1) {
            LOG.error("criteria.xml must contain exactly one element under the xmcda tag and contains: {}", jaxbContents.size());
            return new ArrayList<>();
        }

        // parse criteria object from JAXB element
        Criteria criteria = (Criteria) jaxbContents.get(0).getValue();
        return criteria.getCriterion();
    }

    /**
     * Return the XMCDA criterion values from the initial-weights-{methodName}.xml file
     *
     * @param methodFileName the name of the file to load the initial criterion values for a MCDA method
     */
    public List<CriterionValue> initializeWeightsForCriterion(String methodFileName) throws JAXBException, IOException, SAXException {
        LOG.debug("Initializing criterion values database for prioritization from local {} file!", methodFileName);

        // retrieve the JAXB elements from the xml file
        List<JAXBElement<?>> jaxbContents = getJaxbContentsFromXmcdaFile(methodFileName);

        // only one element is allowed to be in the file
        if (jaxbContents.size() != 1) {
            LOG.error("{} must contain exactly one element under the xmcda tag and contains: {}", methodFileName, jaxbContents.size());
            return new ArrayList<>();
        }

        // parse criteria values object from JAXB element
        CriteriaValues criteriaValues = (CriteriaValues) jaxbContents.get(0).getValue();

        LOG.info("Found {} criterion values in resource file!", criteriaValues.getCriterionValue().size());
        List<CriterionValue> criterionValueList = new ArrayList<>();
        for (org.xmcda.v2.CriterionValue criterionValue : criteriaValues.getCriterionValue()) {

            // add criterion value to criteria and update repository
            CriterionValue internalCriterionValue = CriterionValue.fromXMCDA(criterionValue);
            internalCriterionValue.setMcdaMethod(methodFileName.split("initial-weights-")[1].split("\\.")[0]);
            criterionValueList.add(internalCriterionValue);
        }
        return criterionValueList;
    }

    private List<JAXBElement<?>> getJaxbContentsFromXmcdaFile(String fileName) throws JAXBException, IOException, SAXException {
        // read the file containing the criteria definition
        File criteriaFile = ResourceUtils.getFile("classpath:xmcda/" + fileName);
        XMCDA xmcda = XMCDAParser.readXMCDA(criteriaFile);

        // retrieve the contained elements
        return xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters();
    }
}
