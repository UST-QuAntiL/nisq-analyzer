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

import java.io.StringWriter;
import javax.xml.bind.JAXB;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.xmcda.v2.XMCDA;

/**
 * Utility to decode/encode XMCDA XML documents to interact with the MCDA web services
 */
@Service
public class XmlUtils {

    /**
     * Transform the given XMCDA document to a string and encode it to enable its usage in other SOAP messages as text element which is required by
     * the MCDA web services
     *
     * @param xmcda the XMCDA element to transform into an encoded string
     * @return the encoded string with the content of the XMCDA element
     */
    public String encodeXMCDA(XMCDA xmcda) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(xmcda, sw);
        String xmlString = sw.toString();
        return StringEscapeUtils.escapeXml10(xmlString);
    }
}