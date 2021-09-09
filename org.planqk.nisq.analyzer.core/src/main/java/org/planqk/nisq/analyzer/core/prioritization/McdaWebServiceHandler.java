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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Utility to interact with the MCDA SOAP services
 */
@Service
public class McdaWebServiceHandler {

    private final static Logger LOG = LoggerFactory.getLogger(McdaWebServiceHandler.class);

    public String invokeMcdaOperation(URL serviceURL, String operationName, Map<String, String> bodyFields) {
        LOG.debug("Invoking operation '{}' on MCDA web service at URL: {}", operationName, serviceURL.toString());

        try {
            // create SOAP message with the XMCDA content
            SOAPMessage soapMessage = createSoapMessage(operationName, bodyFields);

            // create SOAP connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // invoke the web service and retrieve the response
            SOAPMessage soapResponse = soapConnection.call(soapMessage, serviceURL);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                soapResponse.writeTo(out);
                LOG.debug(new String(out.toByteArray()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            // TODO
            return null;
        } catch (SOAPException e) {
            LOG.error("Error while invoking MCDA web service: {}", e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Create a new SOAP message with the given operation name as root element in the body and add the given fields as children
     *
     * @param operationName the operation name to use as root element in the SOAP message
     * @param bodyFields    the fields that should be added to the SOAP body under the operation element with the tag name as key and the string
     *                      content as value
     * @return              the created SOAP message with the given contens
     * @throws SOAPException execption if the creation of the SOAP message fails
     */
    private SOAPMessage createSoapMessage(String operationName, Map<String, String> bodyFields) throws SOAPException {

        // create new SOAP message and retrieve body
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();

        // add namespace declaration
        SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");

        // add root element with operation name
        SOAPBody soapBody = soapMessage.getSOAPBody();
        SOAPElement rootElement = soapBody.addBodyElement(QName.valueOf(operationName));

        // add all defined body fields
        for (String bodyFieldKey : bodyFields.keySet()) {
            SOAPElement bodyField = rootElement.addChildElement(bodyFieldKey);

            // the MCDA web services require to add the XMCDA XML files as text fields
            bodyField.addAttribute(QName.valueOf("xsi:type"), "xsd:string");
            bodyField.addTextNode(bodyFields.get(bodyFieldKey));
        }

        return soapMessage;
    }
}
