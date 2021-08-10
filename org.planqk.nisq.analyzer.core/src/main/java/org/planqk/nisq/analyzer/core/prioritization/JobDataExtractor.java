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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Utility to retrieve data of a NISQ Analyzer job, i.e., Analysis Job, Compilation Job, or QPU Selection Job
 */
@Service
public class JobDataExtractor {

    private final static Logger LOG = LoggerFactory.getLogger(JobDataExtractor.class);

    public JobInformation getJobInformationFromUuid(UUID jobId) {
        LOG.debug("Retrieving job information about job with ID: {}", jobId);

        //TODO: retrieve data

        return null;
    }
}
