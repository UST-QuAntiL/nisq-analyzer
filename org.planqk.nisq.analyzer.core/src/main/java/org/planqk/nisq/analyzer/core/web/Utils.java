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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            String[] cmdArray = new String[13];
            cmdArray[0] = "curl";
            cmdArray[1] = "--location";
            cmdArray[2] = "--request";
            cmdArray[3] = "POST";
            cmdArray[4] = "https://platform.planqk.de/auth/realms/planqk/protocol/openid-connect/token";
            cmdArray[5] = "--header";
            cmdArray[6] = "'Content-Type: application/x-www-form-urlencoded'";
            cmdArray[7] = "--data-urlencode";
            cmdArray[8] = "grant_type=refresh_token";
            cmdArray[9] = "--data-urlencode";
            cmdArray[10] = "client_id=vue-frontend";
            cmdArray[11] = "--data-urlencode";
            cmdArray[12] = "refresh_token=" + refreshToken;

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            Process proc = pb.start();
            InputStream inputStream = proc.getInputStream();

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            for (int result = bis.read(); result != -1; result = bis.read()) {
                buf.write((byte) result);
            }

            String jsonString = buf.toString(StandardCharsets.UTF_8.name());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(jsonString);

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
