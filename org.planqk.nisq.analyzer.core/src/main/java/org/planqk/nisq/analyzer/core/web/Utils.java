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

package org.planqk.nisq.analyzer.core.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

public class Utils {

    private final static Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static File inputStreamToFile(InputStream in, String fileEnding) throws IOException {
        final File tempFile = File.createTempFile("temp", "." + fileEnding);
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
            return tempFile;
        } catch (IOException e) {
            return null;
        }
    }

    public static File getFileObjectFromMultipart(MultipartFile multipartFile) {
        try {
            String[] fileNameParts = multipartFile.getOriginalFilename().split("\\.");
            String fileEnding = fileNameParts[fileNameParts.length - 1];
            return Utils.inputStreamToFile(multipartFile.getInputStream(), fileEnding);
        } catch (IOException e) {
            LOG.warn("Exception while loading file from multipart object: {}", e.getLocalizedMessage());
            return null;
        }
    }

    public static File getFileObjectFromUrl(URL url, String refreshToken) {
        try {
            String[] fileNameParts = url.toString().split("/");
            String fileEnding = fileNameParts[fileNameParts.length - 1];

            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            if (url.getHost().equals("platform.planqk.de")) {
                String bearerToken = getBearerTokenFromRefreshToken(refreshToken)[0];
                con.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }

            return Utils.inputStreamToFile(con.getInputStream(), fileEnding);
        } catch (IOException e) {
            LOG.warn("Exception while loading file from URL: {}", e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Gets new bearer token and refresh token from the PlanQK platform.
     * @param refreshToken valid refresh token from the PlanQK platform.
     * @return string array: [bearer token, refresh token]
     */
    public static String[] getBearerTokenFromRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.equals("")) {
            LOG.info("No refresh token provided, cannot get bearer token.");
            return new String[] {"", ""};
        }

        try {
            String url = "https://platform.planqk.de/auth/realms/planqk/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
            map.add("grant_type", "refresh_token");
            map.add("client_id", "vue-frontend");
            map.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity( url, request , String.class );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.getBody());

            if (json.has("error")) {
                LOG.error("Could not get new tokens. Received error message: " + json.at("/error_description").asText());
                return new String[0];
            }

            return new String[] {
                json.at("/access_token").asText(),
                json.at("/refresh_token").asText()
            };
        } catch (Exception e) {
            System.err.println(e);

            return new String[0];
        }
    }
}
