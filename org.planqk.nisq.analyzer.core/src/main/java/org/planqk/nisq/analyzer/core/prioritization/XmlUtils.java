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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.planqk.nisq.analyzer.core.model.DynamicXmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xmcda.v2.AlternativeValue;
import org.xmcda.v2.AlternativesValues;
import org.xmcda.v2.XMCDA;

/**
 * Utility to decode/encode XMCDA XML documents to interact with the MCDA web services
 */
@Service
public class XmlUtils {

    final private static Logger LOG = LoggerFactory.getLogger(XmlUtils.class);

    /**
     * Get the string representation of the given XMCDA document
     *
     * @param xmcda the XMCDA element
     * @return the string representing the XMCDA document
     */
    public String xmcdaToString(XMCDA xmcda) {
        try {
            StringWriter sw = new StringWriter();
            JAXBContext jc = JAXBContext.newInstance(XMCDA.class, DynamicXmlElement.class);
            Marshaller m = jc.createMarshaller();
            m.marshal(xmcda, sw);
            return sw.toString();
        } catch (JAXBException e) {
            LOG.error("Unable to serialize given XMCDA to string!");
            return "";
        }
    }

    /**
     * Change the version of XMCDA objects to address the different versions required by the corresponding web services
     *
     * @param xmcda           the string representation of the XMCDA object to change the version
     * @param baseNamespace   the old namespace of the XMCDA object to adapt
     * @param targetNamespace the new namespace to use
     * @return the string representation of the XMCDA object with updated namespace
     */
    public String changeXMCDAVersion(String xmcda, String baseNamespace, String targetNamespace) {
        return xmcda.replace("=\"" + baseNamespace, "=\"" + targetNamespace);
    }

    /**
     * Get the XMCDA object based on a corresponding XML string
     *
     * @param xmcdaString the XML string to parse
     * @return the resulting XMCDA object
     */
    public XMCDA stringToXmcda(String xmcdaString) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(XMCDA.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xmcdaString);
            return (XMCDA) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            LOG.error("Unable to generate XMCDA object from string: {}", xmcdaString);
            return null;
        }
    }

    /**
     * Get the list of alternative values for a given XMCDA document
     *
     * @param xmcda the XMCDA document to extract the alternative values
     * @return the list of alternative values
     */
    public List<AlternativeValue> getAlternativeValues(XMCDA xmcda) {

        // retrieve the content of the given XMCDA objects
        List<JAXBElement<?>> xmcdaJaxb = xmcda.getProjectReferenceOrMethodMessagesOrMethodParameters();
        if (xmcdaJaxb.isEmpty()) {
            LOG.error("XMCDA document is empty!");
            return null;
        }

        // get the root element of the flows which have to be of class AlternativesValues
        Object xmcdaRoot = xmcdaJaxb.get(0).getValue();
        if (!(xmcdaRoot instanceof AlternativesValues)) {
            LOG.error("Root element is not of type AlternativesValues!");
            return null;
        }

        // cast to AlternativesValues and extract content
        return ((AlternativesValues) xmcdaRoot).getAlternativeValue();
    }

    /**
     * Get the first Value object as double from the given AlternativeValue object
     *
     * @param alternativeValue the AlternativeValue object to retrieve the Value for
     * @return the double from the found Value, or 0 if non is found
     */
    public double getValue(AlternativeValue alternativeValue) {
        if (alternativeValue.getValueOrValues().isEmpty()) {
            LOG.error("AlternativeValue does not contain value!");
            return 0;
        }
        Object valueOrValues = alternativeValue.getValueOrValues().get(0);

        // retrieve contained value or values element
        if (valueOrValues instanceof org.xmcda.v2.Value) {
            return ((org.xmcda.v2.Value) valueOrValues).getReal();
        } else if (valueOrValues instanceof org.xmcda.v2.Values) {
            org.xmcda.v2.Values values = ((org.xmcda.v2.Values) valueOrValues);
            if (values.getValue().isEmpty()) {
                LOG.error("Values element does not contain Value as child!");
                return 0;
            } else {
                return values.getValue().get(0).getReal();
            }
        } else {
            LOG.error("AlternativeValue contains neither Value nor Values as child!");
            return 0;
        }
    }
}
