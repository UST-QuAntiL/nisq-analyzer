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

package org.planqk.nisq.analyzer.core.translator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.planqk.nisq.analyzer.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class TranslatorService {

    final private static Logger LOG = LoggerFactory.getLogger(TranslatorService.class);

    private URI translateAPIEndpoint;

    public TranslatorService(
            @Value("${org.planqk.nisq.analyzer.translator.hostname}") String hostname,
            @Value("${org.planqk.nisq.analyzer.translator.port}") int port
    ) {
        translateAPIEndpoint = URI.create(String.format("http://%s:%d/convert", hostname, port));
    }

    /**
     * Translate the given quantum circuit into an equivalent circuit in the target language
     *
     * @param circuit        the file containing the quantum circuit to translate
     * @param sourceLanguage the language of the input quantum circuit
     * @param targetLanguage the language to which the quantum circuit should be translated
     * @return the file containing the quantum circuit in the target language or null if an error occurs
     */
    public File tranlateCircuit(File circuit, String sourceLanguage, String targetLanguage) {
        LOG.debug("Translating circuit from source language '{}' to target language '{}'!", sourceLanguage, targetLanguage);

        try {
            RestTemplate restTemplate = new RestTemplate();
            TranslationRequest request = new TranslationRequest(sourceLanguage, targetLanguage, FileUtils.readFileToString(circuit, StandardCharsets.UTF_8));

            // translate the circuit into the target language
            ResponseEntity<String> response = restTemplate.postForEntity(translateAPIEndpoint, request, String.class);

            // Check if the Qiskit service was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                LOG.debug("Circuit translated successfully!");

                final File translatedCircuit = File.createTempFile("temp", null);
                FileUtils.writeStringToFile(translatedCircuit, response.getBody());
                return translatedCircuit;
            } else {
                LOG.error(String.format("Error while translating circuit: {}", response.getStatusCodeValue()));
                return null;
            }
        } catch (RestClientException e) {
            LOG.error("Connection to translator service failed.");
            return null;
        } catch (IOException e) {
            LOG.error("Error while reading circuit file.");
            return null;
        }
    }

    /**
     * Get the list of languages that are supported by the translator as input and output format
     *
     * @return the list of supported languages
     */
    public List<String> getSupportedLanguages() {
        return Arrays.asList(Constants.QISKIT, Constants.OPENQASM, Constants.QUIL, Constants.PYQUIL);
    }
}
