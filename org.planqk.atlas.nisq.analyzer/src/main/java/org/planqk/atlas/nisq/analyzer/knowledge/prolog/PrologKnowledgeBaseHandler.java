/*******************************************************************************
 * Copyright (c) 2020 University of Stuttgart
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

package org.planqk.atlas.nisq.analyzer.knowledge.prolog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.jpl7.PrologException;
import org.jpl7.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Class to access and change the local Prolog knowledge base.
 */
@Service
public class PrologKnowledgeBaseHandler {

    final private static Logger LOG = LoggerFactory.getLogger(PrologKnowledgeBaseHandler.class);

    /**
     * Activate the prolog facts and rules contained in the given file
     *
     * @param fileName the name of the file containing the prolog facts and rules
     * @throws UnsatisfiedLinkError Is thrown if the jpl driver is not on the java class path
     */
    public void activatePrologFile(String fileName) throws UnsatisfiedLinkError {
        String activateQuery = "consult('" + Constants.basePath + File.separator + fileName + ".pl').";

        // replace backslashes if running on windows as JPL cannot handle this
        activateQuery = activateQuery.replace("\\", "/");

        // deactivate file in knowledge base
        LOG.debug("Activation of file {} in knowledge base returned: {}", fileName, hasSolution(activateQuery));
    }

    /**
     * Write a Prolog file with the given content to the local directory
     *
     * @param content  the Prolog content to write to the file
     * @param fileName the name of the file to create
     * @throws IOException is thrown in case the writing fails
     */
    public void persistPrologFile(String content, String fileName) throws IOException {
        File file = new File(Constants.basePath + File.separator + fileName + ".pl");
        file.deleteOnExit();
        try {
            File dir = new File(Constants.basePath);
            if (!dir.exists()) dir.mkdirs();
            Writer writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new IOException("Could not write facts to prolog file: " + e.getMessage(), e);
        }
    }

    /**
     * Deactivate the facts and rules contained in the given Prolog file and delete the file.
     *
     * @param fileName the name of the Prolog file
     */
    public void deletePrologFile(String fileName) {
        String deactivateQuery = "unload_file('" + Constants.basePath + File.separator + fileName + ".pl').";

        // replace backslashes if running on windows as JPL cannot handle this
        deactivateQuery = deactivateQuery.replace("\\", "/");

        // deactivate file in knowledge base
        LOG.debug("Deactivation of file {} in knowledge base returned: {}", fileName, hasSolution(deactivateQuery));

        // delete the file
        File file = new File(Constants.basePath + File.separator + fileName + ".pl");
        LOG.debug("Deleting prolog file successful: {}", file.delete());
    }

    /**
     * Check if the prolog file with the given name exists in the knowledge base directory
     *
     * @param fileName the name of the file
     * @return <code>true</code> if the file exists, <code>false</code> otherwise
     */
    public boolean doesPrologFileExist(String fileName) {
        File ruleFile = new File(Constants.basePath + File.separator + fileName + ".pl");
        return ruleFile.exists();
    }

    /**
     * Execute a prolog query and return the evaluation result as boolean
     *
     * @param queryContent the content of the query
     * @return <code>true</code> if there is a solution for the query, <code>false</code> otherwise
     * @throws UnsatisfiedLinkError Is thrown if the jpl driver is not on the java class path
     */
    public boolean hasSolution(String queryContent) throws UnsatisfiedLinkError {
        LOG.debug("Checking if solution for query with the following content exists: {}", queryContent);
        try {
            return Query.hasSolution(queryContent);
        } catch (PrologException e) {
            LOG.warn("Prolog error while executing query. Procedure may not exist in knowledge base...");
            return false;
        }
    }
}
