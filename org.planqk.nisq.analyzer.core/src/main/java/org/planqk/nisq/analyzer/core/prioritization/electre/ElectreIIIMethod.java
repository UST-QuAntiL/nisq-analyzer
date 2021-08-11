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

package org.planqk.nisq.analyzer.core.prioritization.electre;

import java.util.Objects;

import org.planqk.nisq.analyzer.core.model.McdaJob;
import org.planqk.nisq.analyzer.core.prioritization.JobDataExtractor;
import org.planqk.nisq.analyzer.core.prioritization.McdaInformation;
import org.planqk.nisq.analyzer.core.prioritization.McdaMethod;
import org.planqk.nisq.analyzer.core.repository.McdaJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Service implementing the Electre III method to prioritize analysis results of the NISQ Analyzer.
 */
@Service
@RequiredArgsConstructor
public class ElectreIIIMethod implements McdaMethod {

    private final static Logger LOG = LoggerFactory.getLogger(ElectreIIIMethod.class);

    private final JobDataExtractor jobDataExtractor;

    private final McdaJobRepository mcdaJobRepository;

    @Override
    public String getName() {
        return "electre-III";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }

    @Override
    public void executeMcdaMethod(McdaJob mcdaJob) {
        LOG.debug("Starting Electre III MCDA method to prioritize job with ID: {}", mcdaJob.getJobId());
        McdaInformation mcdaInformation = jobDataExtractor.getJobInformationFromUuid(mcdaJob.getJobId());

        // abort if job can not be found and therefore no information available
        if (Objects.isNull(mcdaInformation)) {
            LOG.error("Unable to retrieve information about job with ID: {}", mcdaJob.getJobId());
            mcdaJob.setState("failed");
            mcdaJob.setReady(true);
            mcdaJobRepository.save(mcdaJob);
            return;
        }

        LOG.debug("Retrieved job information contains {} alternatives and {} performances!", mcdaInformation.getAlternatives(), mcdaInformation.getPerformances());

        // TODO: perform Electre
    }
}
