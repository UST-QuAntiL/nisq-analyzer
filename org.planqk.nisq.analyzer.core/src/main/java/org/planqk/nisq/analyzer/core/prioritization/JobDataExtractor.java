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

import java.util.Optional;
import java.util.UUID;

import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.CompilationJob;
import org.planqk.nisq.analyzer.core.model.CompilationResult;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.model.QpuSelectionResult;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.CompilationJobRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xmcda.v2.Alternative;
import org.xmcda.v2.Alternatives;
import org.xmcda.v2.ObjectFactory;
import org.xmcda.v2.PerformanceTable;
import org.xmcda.v2.XMCDA;

import lombok.RequiredArgsConstructor;

/**
 * Utility to retrieve data of a NISQ Analyzer job, i.e., Analysis Job, Compilation Job, or QPU Selection Job
 */
@Service
@RequiredArgsConstructor
public class JobDataExtractor {

    private final static Logger LOG = LoggerFactory.getLogger(JobDataExtractor.class);

    private final QpuSelectionJobRepository qpuSelectionJobRepository;

    private final AnalysisJobRepository analysisJobRepository;

    private final CompilationJobRepository compilationJobRepository;

    /**
     * Get the required information to run MCDA methods from different kinds of NISQ Analyzer jobs
     *
     * @param jobId the ID of the job to retrieve the information from
     * @return the retrieved job information
     */
    public McdaInformation getJobInformationFromUuid(UUID jobId) {
        LOG.debug("Retrieving job information about job with ID: {}", jobId);

        Optional<QpuSelectionJob> qpuSelectionJobOptional = qpuSelectionJobRepository.findById(jobId);
        if (qpuSelectionJobOptional.isPresent()) {
            LOG.debug("Retrieving information from QPU selection job!");
            return getFromQpuSelection(qpuSelectionJobOptional.get());
        }

        Optional<AnalysisJob> analysisJobOptional = analysisJobRepository.findById(jobId);
        if (analysisJobOptional.isPresent()) {
            LOG.debug("Retrieving information from analysis job!");
            return getFromAnalysis(analysisJobOptional.get());
        }

        Optional<CompilationJob> compilationJobOptional = compilationJobRepository.findById(jobId);
        if (compilationJobOptional.isPresent()) {
            LOG.debug("Retrieving information from compilation job!");
            return getFromCompilation(compilationJobOptional.get());
        }

        LOG.error("Unable to find QPU selection, analysis, or compilation job for ID: {}", jobId);
        return null;
    }

    private McdaInformation getFromQpuSelection(QpuSelectionJob qpuSelectionJob) {
        if (!qpuSelectionJob.isReady()) {
            LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
            return null;
        }

        // retrieve required information for the alternatives and performances
        Alternatives alternatives = new Alternatives();
        PerformanceTable performances = new PerformanceTable();
        LOG.debug("QPU selection job contains {} results for the ranking!", qpuSelectionJob.getJobResults().size());
        for (QpuSelectionResult result : qpuSelectionJob.getJobResults()) {
            Alternative alternative = new Alternative();

            // TODO: add required information

            alternatives.getDescriptionOrAlternative().add(alternative);
        }
        LOG.debug("Retrieved job information contains {} alternatives and {} performances!", alternatives.getDescriptionOrAlternative().size(),
                performances.getAlternativePerformances().size());

        return wrapMcdaInformation(alternatives, performances);
    }

    private McdaInformation getFromAnalysis(AnalysisJob analysisJob) {
        if (!analysisJob.isReady()) {
            LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
            return null;
        }

        // retrieve required information for the alternatives and performances
        Alternatives alternatives = new Alternatives();
        PerformanceTable performances = new PerformanceTable();
        LOG.debug("Analysis job contains {} results for the ranking!", analysisJob.getJobResults().size());
        for (AnalysisResult result : analysisJob.getJobResults()) {
            // TODO
            LOG.debug(String.valueOf(result));
        }
        LOG.debug("Retrieved job information contains {} alternatives and {} performances!", alternatives.getDescriptionOrAlternative().size(),
                performances.getAlternativePerformances().size());

        return wrapMcdaInformation(alternatives, performances);
    }

    private McdaInformation getFromCompilation(CompilationJob compilationJob) {
        if (!compilationJob.isReady()) {
            LOG.error("MCDA method execution only possible for finished NISQ Analyzer job but provided job is still running!");
            return null;
        }

        // retrieve required information for the alternatives and performances
        Alternatives alternatives = new Alternatives();
        PerformanceTable performances = new PerformanceTable();
        LOG.debug("Compilation job contains {} results for the ranking!", compilationJob.getJobResults().size());
        for (CompilationResult result : compilationJob.getJobResults()) {
            // TODO
            LOG.debug(String.valueOf(result));
        }
        LOG.debug("Retrieved job information contains {} alternatives and {} performances!", alternatives.getDescriptionOrAlternative().size(),
                performances.getAlternativePerformances().size());

        return wrapMcdaInformation(alternatives, performances);
    }

    /**
     * Wrap the alternatives and performances in XMCDA and JAXB objects and return a corresponding McdaInformation object
     *
     * @param alternatives the alternatives retrieved from a NISQ Analyzer job
     * @param performances the performances retrieved from a NISQ Analyzer job
     * @return the McdaInformation object containing all required information to invoke the MCDA web services
     */
    private McdaInformation wrapMcdaInformation(Alternatives alternatives, PerformanceTable performances) {
        ObjectFactory objectFactory = new ObjectFactory();

        // create XMCDA wrapper containing the alternatives and performances required by the MCDA web services
        XMCDA alternativesWrapper = objectFactory.createXMCDA();
        alternativesWrapper.getProjectReferenceOrMethodMessagesOrMethodParameters().add(objectFactory.createXMCDAAlternatives(alternatives));
        XMCDA performancesWrapper = objectFactory.createXMCDA();
        performancesWrapper.getProjectReferenceOrMethodMessagesOrMethodParameters().add(objectFactory.createXMCDAPerformanceTable(performances));

        // return all information to invoke the MCDA services
        McdaInformation mcdaInformation = new McdaInformation();
        mcdaInformation.setAlternatives(alternativesWrapper);
        mcdaInformation.setPerformances(performancesWrapper);
        return mcdaInformation;
    }
}
