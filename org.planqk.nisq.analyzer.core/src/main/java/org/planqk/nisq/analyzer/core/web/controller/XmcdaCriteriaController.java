/*******************************************************************************
 * Copyright (c) 2024 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.web.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.ExecutionResultStatus;
import org.planqk.nisq.analyzer.core.model.JobType;
import org.planqk.nisq.analyzer.core.model.McdaJob;
import org.planqk.nisq.analyzer.core.model.McdaResult;
import org.planqk.nisq.analyzer.core.model.McdaSensitivityAnalysisJob;
import org.planqk.nisq.analyzer.core.model.McdaWeightLearningJob;
import org.planqk.nisq.analyzer.core.model.xmcda.CriterionValue;
import org.planqk.nisq.analyzer.core.prioritization.McdaMethod;
import org.planqk.nisq.analyzer.core.prioritization.restMcdaAndPrediction.PrioritizationService;
import org.planqk.nisq.analyzer.core.repository.McdaJobRepository;
import org.planqk.nisq.analyzer.core.repository.McdaSensitivityAnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.McdaWeightLearningJobRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionResultRepository;
import org.planqk.nisq.analyzer.core.repository.xmcda.XmcdaRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.McdaCriterionDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.McdaCriterionListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.McdaMethodDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.McdaMethodListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xmcda.v2.Criterion;
import org.xmcda.v2.Scale;
import org.xmcda.v2.Value;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Tag(name = "xmcda-criteria")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.MCDA_METHODS)
public class XmcdaCriteriaController {

    private final static Logger LOG = LoggerFactory.getLogger(XmcdaCriteriaController.class);

    final private List<McdaMethod> mcdaMethods;

    final private PrioritizationService prioritizationService;

    final private XmcdaRepository xmcdaRepository;

    final private McdaJobRepository mcdaJobRepository;

    final private McdaSensitivityAnalysisJobRepository mcdaSensitivityAnalysisJobRepository;

    final private McdaWeightLearningJobRepository mcdaWeightLearningJobRepository;

    final private QpuSelectionResultRepository qpuSelectionResultRepository;

    @Operation(responses = {
        @ApiResponse(responseCode = "200")}, description = "Get all supported prioritization methods")
    @GetMapping("/")
    public HttpEntity<McdaMethodListDto> getSupportedPrioritizationMethods() {
        LOG.debug("Retrieving all supported MCDA methods!");
        McdaMethodListDto model = new McdaMethodListDto();

        // add all supported methods and corresponding links
        for (McdaMethod mcdaMethod : mcdaMethods) {
            model.add(createMcdaMethodDto(mcdaMethod));
            model.add(
                linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationMethod(mcdaMethod.getName())).withRel(
                    mcdaMethod.getName()));
        }

        // add self link
        model.add(linkTo(methodOn(XmcdaCriteriaController.class).getSupportedPrioritizationMethods()).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve a single prioritization " +
        "method")
    @GetMapping("/{methodName}")
    public HttpEntity<McdaMethodDto> getPrioritizationMethod(@PathVariable String methodName) {
        LOG.debug("Retrieving MCDA method with name: {}", methodName);
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst();

        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createMcdaMethodDto(optional.get()), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve a single prioritization " +
        "method")
    @GetMapping("/{methodName}/" + Constants.CRITERIA)
    public HttpEntity<McdaCriterionListDto> getCriterionForMethod(@PathVariable String methodName) {
        LOG.debug("Retrieving criteria for MCDA method with name: {}", methodName);
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst();

        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // get dtos for all criterion defined for this MCDA method
        List<McdaCriterionDto> mcdaCriterionDtos = xmcdaRepository.findByMcdaMethod(methodName).stream()
            .map(criterion -> createMcdaCriterionDto(criterion, methodName)).collect(Collectors.toList());

        McdaCriterionListDto mcdaCriterionListDto = new McdaCriterionListDto();
        mcdaCriterionListDto.add(mcdaCriterionDtos);
        mcdaCriterionListDto.add(
            linkTo(methodOn(XmcdaCriteriaController.class).getCriterionForMethod(methodName)).withSelfRel());
        return new ResponseEntity<>(mcdaCriterionListDto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve a single criterion for a " +
        "MCDA method")
    @GetMapping("/{methodName}/" + Constants.CRITERIA + "/{criterionId}")
    public HttpEntity<McdaCriterionDto> getCriterion(@PathVariable String methodName,
                                                     @PathVariable String criterionId) {

        // get dtos for all criterion defined for this MCDA method
        Optional<McdaCriterionDto> mcdaCriterionDto = xmcdaRepository.findByMcdaMethod(methodName).stream()
            .filter(criterion -> criterion.getId().equals(criterionId))
            .map(criterion -> createMcdaCriterionDto(criterion, methodName)).findFirst();

        if (!mcdaCriterionDto.isPresent()) {
            LOG.error("Unable to find criterion with id {} for MCDA method: {}", criterionId, methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(mcdaCriterionDto.get(), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve the criterion value for a " +
        "MCDA method")
    @GetMapping("/{methodName}/" + Constants.CRITERIA + "/{criterionId}/" + Constants.CRITERIA_VALUE)
    public HttpEntity<EntityModel<CriterionValue>> getCriterionValue(@PathVariable String methodName,
                                                                     @PathVariable String criterionId) {

        Optional<EntityModel<CriterionValue>> mcdaCriterionValueDto =
            xmcdaRepository.findByCriterionIdAndMethod(criterionId, methodName)
                .map(criterionValue -> createMcdaCriterionValueDto(criterionValue, methodName));

        if (!mcdaCriterionValueDto.isPresent()) {
            LOG.error("Unable to find criterion value for criterion with id {} and MCDA method: {}", criterionId,
                methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(mcdaCriterionValueDto.get(), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve the criterion value for a " +
        "MCDA method")
    @PutMapping("/{methodName}/" + Constants.CRITERIA + "/{criterionId}/" + Constants.CRITERIA_VALUE)
    public HttpEntity<EntityModel<CriterionValue>> updateCriterionValue(@PathVariable String methodName,
                                                                        @PathVariable String criterionId,
                                                                        @RequestBody CriterionValue criterionValue) {

        // find existing entity that should be updated
        Optional<EntityModel<CriterionValue>> mcdaCriterionValueDto =
            xmcdaRepository.findByCriterionIdAndMethod(criterionId, methodName)
                .map(value -> createMcdaCriterionValueDto(criterionValue, methodName));

        if (!mcdaCriterionValueDto.isPresent()) {
            LOG.error("Unable to find criterion value for criterion with id {} and MCDA method: {}", criterionId,
                methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (!criterionValue.getCriterionID().equals(criterionId) ||
            !criterionValue.getMcdaMethod().equals(methodName)) {
            LOG.error("Updated criterion value must specify correct criterion id and MCDA method name!");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // check if criterion value contains exactly one value
        if (criterionValue.getValueOrValues().size() != 1) {
            LOG.error("Criterion value must specify exactly one value!");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Object object = criterionValue.getValueOrValues().get(0);

        // parse LinkedHashMap to Value object if serialization failed
        if (object.getClass().equals(java.util.LinkedHashMap.class)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Value value = mapper.convertValue(object, Value.class);
                criterionValue.getValueOrValues().remove(object);
                criterionValue.getValueOrValues().add(value);
            } catch (Exception e) {
                LOG.error("Unable to parse contained LinkedHashMap to Value object!");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        xmcdaRepository.updateCriterionValue(criterionValue);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve all MCDA jobs for the given " +
        "method")
    @GetMapping("/{methodName}/" + Constants.JOBS)
    public HttpEntity<CollectionModel<EntityModel<McdaJob>>> getPrioritizationJobs(@PathVariable String methodName) {
        LOG.debug("Retrieving all jobs for MCDA method with name: {}", methodName);

        // check if method is supported
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst();
        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // get all related jobs
        List<EntityModel<McdaJob>> jobs = new ArrayList<>();
        for (McdaJob mcdaJob : mcdaJobRepository.findByMethod(methodName)) {
            EntityModel<McdaJob> mcdaJobDto = new EntityModel<>(mcdaJob);
            addLinksToRelatedResults(mcdaJobDto, mcdaJob);
            mcdaJobDto.add(linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationJob(methodName,
                mcdaJob.getJobId())).withSelfRel());
            jobs.add(mcdaJobDto);
        }

        CollectionModel<EntityModel<McdaJob>> dto = new CollectionModel<>(jobs);
        dto.add(linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationJobs(methodName)).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve all MCDA jobs for the given " +
        "method")
    @GetMapping("/{methodName}/" + Constants.JOBS + "/{jobId}")
    public HttpEntity<EntityModel<McdaJob>> getPrioritizationJob(@PathVariable String methodName,
                                                                 @PathVariable UUID jobId) {
        LOG.debug("Retrieving MCDA job with ID: {}", jobId);

        // check if method is supported
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst();
        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // search for job
        Optional<McdaJob> jobOptional = mcdaJobRepository.findById(jobId);
        if (!jobOptional.isPresent()) {
            LOG.error("Job with ID {} not found.", jobId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        McdaJob job = jobOptional.get();
        if (!job.getMethod().equals(methodName)) {
            LOG.error("Job with ID {} does not belong to method: {}", jobId, methodName);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        EntityModel<McdaJob> mcdaJobDto = new EntityModel<>(job);
        addLinksToRelatedResults(mcdaJobDto, job);
        mcdaJobDto.add(
            linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationJob(methodName, jobId)).withSelfRel());
        return new ResponseEntity<>(mcdaJobDto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Run the MCDA method on the NISQ " +
        "Analyzer job passed as parameter")
    @PostMapping(value = "/{methodName}/" + Constants.MCDA_PRIORITIZE)
    public HttpEntity<EntityModel<McdaJob>> prioritizeCompiledCircuitsOfJob(@PathVariable String methodName,
                                                                            @RequestParam UUID jobId,
                                                                            @RequestParam Boolean useBordaCount,
                                                                            @RequestParam Float queueImportanceRatio) {
        LOG.debug("Creating new job to run prioritization with MCDA method {} and NISQ Analyzer job with ID: {}",
            methodName, jobId);

        // check if method is supported
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst();
        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        McdaMethod mcdaMethod = optional.get();

        if (methodName.equals("electre-III") && useBordaCount) {
            LOG.error("MCDA method with name {} does not support Borda Count.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Map<String, Float> bordaCountWeights = new HashMap<>();

        if (useBordaCount && queueImportanceRatio == null) {
            LOG.error("A ratio is required when Borda Count should be applied.");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else if (useBordaCount) {
            bordaCountWeights.put("queue-size", queueImportanceRatio);
            bordaCountWeights.put("result_precision", 1 - queueImportanceRatio);
        } else {
            bordaCountWeights.put("queue-size", 0.0f);
            bordaCountWeights.put("result_precision", 0.0f);
        }

        // create job object and pass to corresponding MCDA plugin
        McdaJob mcdaJob = new McdaJob();
        mcdaJob.setTime(OffsetDateTime.now());
        mcdaJob.setMethod(methodName);
        mcdaJob.setUseBordaCount(useBordaCount);
        mcdaJob.setBordaCountWeights(bordaCountWeights);
        mcdaJob.setReady(false);
        mcdaJob.setJobId(jobId);
        mcdaJob.setState(ExecutionResultStatus.INITIALIZED.toString());

        // store object to generate UUID
        McdaJob storedMcdaJob = mcdaJobRepository.save(mcdaJob);

        new Thread(() -> {
            mcdaMethod.executeMcdaMethod(storedMcdaJob);
        }).start();

        // return dto with link to poll for updates
        EntityModel<McdaJob> mcdaJobDto = new EntityModel<>(storedMcdaJob);
        mcdaJobDto.add(linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationJob(methodName,
            storedMcdaJob.getId())).withSelfRel());
        return new ResponseEntity<>(mcdaJobDto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve all MCDA-weight-learning " +
        "jobs for the given method")
    @GetMapping(
        "/{methodName}/" + Constants.WEIGHT_LEARNING_METHODS + "/{weightLearningMethod}/" + Constants.JOBS + "/{jobId}")
    public HttpEntity<EntityModel<McdaWeightLearningJob>> getWeightLearningJob(@PathVariable String methodName,
                                                                               @PathVariable
                                                                               String weightLearningMethod,
                                                                               @PathVariable UUID jobId) {
        LOG.debug("Retrieving MCDA-weight-learning job with ID: {}", jobId);

        // check if MCDA method is supported
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst()
                .filter(mcdaMethod -> !mcdaMethod.getName().equals("electre-III"));
        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported to learn weights.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // check if weighting method is supported
        if (!weightLearningMethod.matches("cobyla|genetic-algorithm|evolution-strategy")) {
            LOG.error("Weight learning method with name {} not supported.", weightLearningMethod);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // search for job
        Optional<McdaWeightLearningJob> jobOptional = mcdaWeightLearningJobRepository.findById(jobId);
        if (!jobOptional.isPresent()) {
            LOG.error("Job with ID {} not found.", jobId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        McdaWeightLearningJob job = jobOptional.get();
        if (!job.getMcdaMethod().equals(methodName)) {
            LOG.error("Job with ID {} does not belong to MCDA method: {}", jobId, methodName);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (!job.getWeightLearningMethod().equals(weightLearningMethod)) {
            LOG.error("Job with ID {} does not belong to weight learning method: {}", jobId, weightLearningMethod);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        EntityModel<McdaWeightLearningJob> mcdaJobDto = new EntityModel<>(job);
        // addLinksToRelatedResults(mcdaJobDto, job); TODO add link to get criteria weights
        mcdaJobDto.add(linkTo(
            methodOn(XmcdaCriteriaController.class).getWeightLearningJob(methodName, weightLearningMethod,
                jobId)).withSelfRel());
        return new ResponseEntity<>(mcdaJobDto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Run the MCDA method and weight " +
        "learning method on the NISQ Analyzer, job passed as parameter")
    @PostMapping(value = "/{methodName}/" + Constants.WEIGHT_LEARNING_METHODS + "/{weightLearningMethod}/" +
        Constants.MCDA_LEARN_WEIGHTS)
    public HttpEntity<EntityModel<McdaWeightLearningJob>> learnWeightsForCompiledCircuitsOfJob(
        @PathVariable String methodName, @PathVariable String weightLearningMethod) {
        LOG.debug("Creating new job to run weight learning with MCDA method {} and weight learning method {}",
            methodName, weightLearningMethod);

        // check if MCDA method is supported
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst()
                .filter(mcdaMethod -> !mcdaMethod.getName().equals("electre-III"));
        ;
        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported to learn weights.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // check if weighting method is supported
        if (!weightLearningMethod.matches("cobyla|genetic-algorithm|evolution-strategy")) {
            LOG.error("Weight learning method with name {} not supported.", weightLearningMethod);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // create job object and pass to corresponding MCDA plugin
        McdaWeightLearningJob mcdaWeightLearningJob = new McdaWeightLearningJob();
        mcdaWeightLearningJob.setTime(OffsetDateTime.now());
        mcdaWeightLearningJob.setMcdaMethod(methodName);
        mcdaWeightLearningJob.setState(ExecutionResultStatus.INITIALIZED.toString());
        mcdaWeightLearningJob.setWeightLearningMethod(weightLearningMethod);
        mcdaWeightLearningJob.setReady(false);

        // store object to generate UUID
        McdaWeightLearningJob storedMcdaWeightLearningJob = mcdaWeightLearningJobRepository.save(mcdaWeightLearningJob);

        new Thread(() -> {
            prioritizationService.learnWeights(storedMcdaWeightLearningJob);
        }).start();

        // return dto with link to poll for updates
        EntityModel<McdaWeightLearningJob> mcdaWeightLearningJobDto = new EntityModel<>(storedMcdaWeightLearningJob);
        mcdaWeightLearningJobDto.add(linkTo(
            methodOn(XmcdaCriteriaController.class).getWeightLearningJob(methodName, weightLearningMethod,
                storedMcdaWeightLearningJob.getId())).withSelfRel());
        return new ResponseEntity<>(mcdaWeightLearningJobDto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve all sensitivity analysis " +
        "jobs for the given method")
    @GetMapping("/{methodName}/" + Constants.MCDA_SENSITIVITY_ANALYZES + "/" + Constants.JOBS)
    public HttpEntity<CollectionModel<EntityModel<McdaSensitivityAnalysisJob>>> getSensitivityAnalysisJobs(
        @PathVariable String methodName) {
        LOG.debug("Retrieving all sensitivity analysis jobs for MCDA method with name: {}", methodName);

        // check if method is supported
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst()
                .filter(mcdaMethod -> !mcdaMethod.getName().equals("electre-III"));
        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported for sensitivity analyzes.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // get all related jobs
        List<EntityModel<McdaSensitivityAnalysisJob>> jobs = new ArrayList<>();
        for (McdaSensitivityAnalysisJob mcdaJob : mcdaSensitivityAnalysisJobRepository.findByMethod(methodName)) {
            EntityModel<McdaSensitivityAnalysisJob> mcdaJobDto = new EntityModel<>(mcdaJob);
            //addLinksToRelatedResults(mcdaJobDto, mcdaJob); TODO
            mcdaJobDto.add(linkTo(methodOn(XmcdaCriteriaController.class).getSensitivityAnalysisJob(methodName,
                mcdaJob.getJobId())).withSelfRel());
            jobs.add(mcdaJobDto);
        }

        CollectionModel<EntityModel<McdaSensitivityAnalysisJob>> dto = new CollectionModel<>(jobs);
        dto.add(linkTo(methodOn(XmcdaCriteriaController.class).getSensitivityAnalysisJobs(methodName)).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", content = @Content)}, description = "Retrieve the sensitivity analysis job" +
        " for the given method")
    @GetMapping("/{methodName}/" + Constants.MCDA_SENSITIVITY_ANALYZES + "/" + Constants.JOBS + "/{jobId}")
    public HttpEntity<EntityModel<McdaSensitivityAnalysisJob>> getSensitivityAnalysisJob(
        @PathVariable String methodName, @PathVariable UUID jobId) {
        LOG.debug("Retrieving sensitivity analysis job with ID: {}", jobId);

        // check if method is supported
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst()
                .filter(mcdaMethod -> !mcdaMethod.getName().equals("electre-III"));
        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported for sensitivity analyzes.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // search for job
        Optional<McdaSensitivityAnalysisJob> jobOptional = mcdaSensitivityAnalysisJobRepository.findById(jobId);
        if (!jobOptional.isPresent()) {
            LOG.error("Job with ID {} not found.", jobId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        McdaSensitivityAnalysisJob job = jobOptional.get();
        if (!job.getMethod().equals(methodName)) {
            LOG.error("Job with ID {} does not belong to method: {}", jobId, methodName);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        EntityModel<McdaSensitivityAnalysisJob> mcdaSensitivityAnalysisJobDto = new EntityModel<>(job);
        //addLinksToRelatedResults(mcdaSensitivityAnalysisJobDto, job); TODO maybe add link to job, therefore jobType
        // is required to call right endpoint
        mcdaSensitivityAnalysisJobDto.add(
            linkTo(methodOn(XmcdaCriteriaController.class).getSensitivityAnalysisJob(methodName, jobId)).withSelfRel());
        return new ResponseEntity<>(mcdaSensitivityAnalysisJobDto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Run the MCDA method on the NISQ " +
        "Analyzer job passed as parameter")
    @PostMapping(value = "/{methodName}/" + Constants.MCDA_SENSITIVITY_ANALYZES + "/" +
        Constants.MCDA_ANALYZE_SENSITIVITY)
    public HttpEntity<EntityModel<McdaSensitivityAnalysisJob>> analyzeSensitivityOfCompiledCircuitsOfJob(
        @PathVariable String methodName, @RequestParam UUID jobId, @RequestParam float stepSize,
        @RequestParam float upperBound, @RequestParam float lowerBound, @RequestParam Boolean useBordaCount,
        @RequestParam Float queueImportanceRatio) {
        LOG.debug("Creating new job to run sensitivity analysis with MCDA method {} and NISQ Analyzer job with ID: {}",
            methodName, jobId);

        Map<String, Float> bordaCountWeights = new HashMap<>();

        if (useBordaCount && queueImportanceRatio == null) {
            LOG.error("A ratio is required when Borda Count should be applied.");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else if (useBordaCount) {
            bordaCountWeights.put("queue-size", queueImportanceRatio);
            bordaCountWeights.put("result_precision", 1 - queueImportanceRatio);
        } else {
            bordaCountWeights.put("queue-size", 0.0f);
            bordaCountWeights.put("result_precision", 0.0f);
        }

        // check if method is supported
        Optional<McdaMethod> optional =
            mcdaMethods.stream().filter(method -> method.getName().equals(methodName)).findFirst()
                .filter(mcdaMethod -> !mcdaMethod.getName().equals("electre-III"));
        if (!optional.isPresent()) {
            LOG.error("MCDA method with name {} not supported for sensitivity analyzes.", methodName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // create job object
        McdaSensitivityAnalysisJob mcdaSensitivityAnalysisJob = new McdaSensitivityAnalysisJob();
        mcdaSensitivityAnalysisJob.setTime(OffsetDateTime.now());
        mcdaSensitivityAnalysisJob.setMethod(methodName);
        mcdaSensitivityAnalysisJob.setReady(false);
        mcdaSensitivityAnalysisJob.setUseBordaCount(useBordaCount);
        mcdaSensitivityAnalysisJob.setBordaCountWeights(bordaCountWeights);
        mcdaSensitivityAnalysisJob.setJobId(jobId);
        mcdaSensitivityAnalysisJob.setStepSize(stepSize);
        mcdaSensitivityAnalysisJob.setUpperBound(upperBound);
        mcdaSensitivityAnalysisJob.setLowerBound(lowerBound);
        mcdaSensitivityAnalysisJob.setState(ExecutionResultStatus.INITIALIZED.toString());

        // store object to generate UUID
        McdaSensitivityAnalysisJob storedMcdaSensitivityAnalysisJob =
            mcdaSensitivityAnalysisJobRepository.save(mcdaSensitivityAnalysisJob);

        new Thread(() -> {
            prioritizationService.analyzeSensitivity(storedMcdaSensitivityAnalysisJob);
        }).start();

        // return dto with link to poll for updates
        EntityModel<McdaSensitivityAnalysisJob> mcdaSensitivityAnalysisJobDto =
            new EntityModel<>(storedMcdaSensitivityAnalysisJob);
        mcdaSensitivityAnalysisJobDto.add(linkTo(
            methodOn(XmcdaCriteriaController.class).getSensitivityAnalysisJob(methodName,
                storedMcdaSensitivityAnalysisJob.getId())).withSelfRel());
        return new ResponseEntity<>(mcdaSensitivityAnalysisJobDto, HttpStatus.OK);
    }

    private McdaMethodDto createMcdaMethodDto(McdaMethod method) {
        McdaMethodDto dto = new McdaMethodDto();
        dto.setName(method.getName());
        dto.setDescription(method.getDescription());
        dto.add(
            linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationMethod(method.getName())).withSelfRel());
        dto.add(linkTo(methodOn(XmcdaCriteriaController.class).getCriterionForMethod(method.getName())).withRel(
            Constants.CRITERIA));
        dto.add(linkTo(methodOn(XmcdaCriteriaController.class).getPrioritizationJobs(method.getName())).withRel(
            Constants.JOBS));
        return dto;
    }

    private McdaCriterionDto createMcdaCriterionDto(Criterion criterion, String methodName) {
        McdaCriterionDto dto = new McdaCriterionDto();
        dto.setMcdaConcept(criterion.getMcdaConcept());
        dto.setId(criterion.getId());
        dto.setName(criterion.getName());
        dto.setDescription(criterion.getDescription());

        // check if criterion is set to active and return false otherwise
        dto.setActive(
            criterion.getActiveOrScaleOrCriterionFunction().stream().filter(object -> object instanceof Boolean)
                .map(object -> (Boolean) object).findFirst().orElse(false));

        // find scale child object to retrieve required information
        dto.setScale(criterion.getActiveOrScaleOrCriterionFunction().stream().filter(object -> object instanceof Scale)
            .map(object -> (Scale) object).findFirst().orElse(null));

        dto.add(
            linkTo(methodOn(XmcdaCriteriaController.class).getCriterion(methodName, criterion.getId())).withSelfRel());
        dto.add(
            linkTo(methodOn(XmcdaCriteriaController.class).getCriterionValue(methodName, criterion.getId())).withRel(
                Constants.CRITERIA_VALUE));
        return dto;
    }

    private EntityModel<CriterionValue> createMcdaCriterionValueDto(CriterionValue criterionValue, String methodName) {
        EntityModel<CriterionValue> dto = new EntityModel<>(criterionValue);
        dto.add(linkTo(methodOn(XmcdaCriteriaController.class).getCriterionValue(methodName,
            criterionValue.getCriterionID())).withSelfRel());
        return dto;
    }

    private void addLinksToRelatedResults(EntityModel<McdaJob> mcdaJobDto, McdaJob job) {
        for (McdaResult mcdaResult : job.getRankedResults()) {
            if (job.getJobType().equals(JobType.ANALYSIS)) {
                mcdaJobDto.add(linkTo(
                    methodOn(AnalysisResultController.class).getAnalysisResult(mcdaResult.getResultId())).withRel(
                    mcdaResult.getResultId().toString()));
            }
            if (job.getJobType().equals(JobType.COMPILATION)) {
                mcdaJobDto.add(linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisResult(
                    mcdaResult.getResultId())).withRel(mcdaResult.getResultId().toString()));
            }
            if (job.getJobType().equals(JobType.QPU_SELECTION)) {
                String qpuSelectionResultUserId =
                    qpuSelectionResultRepository.findById(mcdaResult.getResultId()).get().getUserId();
                mcdaJobDto.add(linkTo(
                    methodOn(QpuSelectionResultController.class).getQpuSelectionResult(mcdaResult.getResultId(),
                        qpuSelectionResultUserId)).withRel(mcdaResult.getResultId().toString()).expand());
            }
        }
    }
}
