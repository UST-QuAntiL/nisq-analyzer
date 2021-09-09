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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
import org.w3c.dom.Node;

/**
 * Utility to interact with the MCDA SOAP services
 */
@Service
public class McdaWebServiceHandler {

    private final static Logger LOG = LoggerFactory.getLogger(McdaWebServiceHandler.class);

    /**
     * Invoke the given operation on the web service available at the given URL with the given body fields
     *
     * @param serviceURL    the URL of the web service to invoke
     * @param operationName the name of the operation to invoke to use as root element of the SOAP message
     * @param bodyFields    a map containing the names of the tags to add as children of the root element and their text content as value
     * @return the different result elements of the web service invocation excluding status information, or null if an error occurs
     */
    public Map<String, String> invokeMcdaOperation(URL serviceURL, String operationName, Map<String, String> bodyFields) {
        LOG.debug("Invoking operation '{}' on MCDA web service at URL: {}", operationName, serviceURL.toString());

        try {
            // create SOAP message with the XMCDA content
            SOAPMessage soapMessage = createSoapMessage(operationName, bodyFields);

            // create SOAP connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // invoke the web service and retrieve the response
            SOAPMessage soapResponse = soapConnection.call(soapMessage, serviceURL);

            // poll for the web service result
            soapResponse = waitForResponse(serviceURL, soapConnection, soapResponse, 10);
            if (Objects.isNull(soapResponse)) {
                LOG.error("Unable to retrieve result from web service within defined timeout! Aborting!");
                return null;
            }

            // get root element of the response
            SOAPBody body = soapResponse.getSOAPBody();
            if (body.getChildNodes().getLength() == 0) {
                LOG.error("Retrieved SOAP message contains body with no elements!");
                return null;
            }
            Node rootElement = body.getChildNodes().item(0);

            LOG.debug("Result of web service invocation contains {} elements!", rootElement.getChildNodes().getLength());
            HashMap<String, String> result = new HashMap<>();
            for (int i = 0; i < rootElement.getChildNodes().getLength(); i++) {
                Node childNode = rootElement.getChildNodes().item(i);

                // skip status information
                if (childNode.getNodeName().equals(McdaConstants.WEB_SERVICE_DATA_TICKET)
                        || childNode.getNodeName().equals(McdaConstants.WEB_SERVICE_DATA_STATUS)
                        || childNode.getNodeName().equals(McdaConstants.WEB_SERVICE_DATA_MESSAGES)) {
                    continue;
                }

                result.put(childNode.getNodeName(), childNode.getTextContent());
            }
            return result;
        } catch (SOAPException e) {
            LOG.error("Error while invoking MCDA web service: {}", e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Poll for the result of the web service until it terminates or the maximum number of iterations is reached
     *
     * @param serviceURL     the URL to use to communicate with the web service
     * @param soapConnection the SOAP connection to use to perform the requests
     * @param soapMessage    the SOAP message containing the ticket to poll for
     * @param maxIterations  the number of iterations to poll for the result until aborting
     * @return the SOAP message with the results or null if the web service did not terminate in time
     */
    private SOAPMessage waitForResponse(URL serviceURL, SOAPConnection soapConnection, SOAPMessage soapMessage, int maxIterations)
            throws SOAPException {

        // get the ticket ID from the SOAP message
        String ticketId = getTicketId(soapMessage);
        if (Objects.isNull(ticketId)) {
            LOG.error("Unable to retrieve ticket ID from SOAP response!");
            return null;
        }
        LOG.debug("Polling for result with ticket ID: {}", ticketId);

        // create SOAP message for the polling
        HashMap<String, String> bodyFields = new HashMap<>();
        bodyFields.put(McdaConstants.WEB_SERVICE_DATA_TICKET, ticketId);
        SOAPMessage pollingMessage = createSoapMessage(McdaConstants.WEB_SERVICE_OPERATIONS_REQUEST_SOLUTION, bodyFields);

        int iteration = 0;
        while (iteration < maxIterations) {
            iteration++;
            LOG.debug("Waiting for 5 seconds for web service result (iteration {} of {})", iteration, maxIterations);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // poll for result
            SOAPMessage soapResponse = soapConnection.call(pollingMessage, serviceURL);
            SOAPBody body = soapResponse.getSOAPBody();
            if (body.getChildNodes().getLength() == 0) {
                continue;
            }
            Node rootElement = body.getChildNodes().item(0);

            LOG.debug("Polling response contains {} children!", rootElement.getChildNodes().getLength());
            for (int i = 0; i < rootElement.getChildNodes().getLength(); i++) {
                Node childNode = rootElement.getChildNodes().item(i);

                // check if status code is unequal to 1, meaning that the service terminated
                if (childNode.getNodeName().equals(McdaConstants.WEB_SERVICE_DATA_STATUS)) {
                    if (Integer.parseInt(childNode.getTextContent()) == 1) {
                        LOG.debug("Web service is still processing the request...");
                    } else {
                        return soapResponse;
                    }
                }
            }
        }

        // return null if web service did not terminate
        return null;
    }

    /**
     * Get the ticket ID of the submitted job from the resulting response SOAP message
     *
     * @param soapMessage the result SOAP message
     * @return the ticket ID if found or null otherwise
     * @throws SOAPException exception if parsing the SOAP message is not possible
     */
    private String getTicketId(SOAPMessage soapMessage) throws SOAPException {
        SOAPBody body = soapMessage.getSOAPBody();
        if (body.getChildNodes().getLength() == 0) {
            LOG.error("Response message does not contain any children which are required to retrieve the ticket ID!");
            return null;
        }

        Node rootElement = body.getChildNodes().item(0);
        for (int i = 0; i < rootElement.getChildNodes().getLength(); i++) {
            Node childNode = rootElement.getChildNodes().item(i);
            if (childNode.getNodeName().equals(McdaConstants.WEB_SERVICE_DATA_TICKET)) {
                return childNode.getTextContent();
            }
        }
        return null;
    }

    /**
     * Create a new SOAP message with the given operation name as root element in the body and add the given fields as children
     *
     * @param operationName the operation name to use as root element in the SOAP message
     * @param bodyFields    the fields that should be added to the SOAP body under the operation element with the tag name as key and the string
     *                      content as value
     * @return the created SOAP message with the given contens
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
