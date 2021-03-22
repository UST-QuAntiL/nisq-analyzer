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
import java.net.URL;

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

    public static File getFileObjectFromUrl(URL url) {
        try {
            String[] fileNameParts = url.toString().split("/");
            String fileEnding = fileNameParts[fileNameParts.length - 1];
            return Utils.inputStreamToFile(url.openStream(), fileEnding);
        } catch (IOException e) {
            LOG.warn("Exception while loading file from URL: {}", e.getLocalizedMessage());
            return null;
        }
    }
}
