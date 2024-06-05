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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.control.NisqAnalyzerControlService;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.CompilationJob;
import org.planqk.nisq.analyzer.core.model.QpuSelectionJob;
import org.planqk.nisq.analyzer.core.repository.AnalysisJobRepository;
import org.planqk.nisq.analyzer.core.repository.CompilationJobRepository;
import org.planqk.nisq.analyzer.core.repository.QpuSelectionJobRepository;
import org.planqk.nisq.analyzer.core.web.Utils;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisJobDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.CompilationJobDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ParameterListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuSelectionJobDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.CompilerSelectionDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.QpuSelectionDto;
import org.planqk.nisq.analyzer.core.web.dtos.requests.SelectionRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Root controller to access all entities within Quality, trigger the hardware selection, and execution of quantum
 * algorithms.
 */
@Tag(name = "root")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
public class RootController {

    private final static Logger LOG = LoggerFactory.getLogger(RootController.class);

    private final NisqAnalyzerControlService nisqAnalyzerService;

    private final CompilationJobRepository compilationJobRepository;

    private final AnalysisJobRepository analysisJobRepository;

    private final QpuSelectionJobRepository qpuSelectionJobRepository;

    public RootController(NisqAnalyzerControlService nisqAnalyzerService,
                          CompilationJobRepository compilationJobRepository,
                          AnalysisJobRepository analysisJobRepository,
                          QpuSelectionJobRepository qpuSelectionJobRepository) {
        this.nisqAnalyzerService = nisqAnalyzerService;
        this.compilationJobRepository = compilationJobRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.qpuSelectionJobRepository = qpuSelectionJobRepository;
    }

    @Operation(responses = {@ApiResponse(responseCode = "200")}, description = "Root operation, returns further links")
    @GetMapping("/")
    public HttpEntity<RepresentationModel> root() {
        RepresentationModel responseEntity = new RepresentationModel<>();

        // add links to sub-controllers
        responseEntity.add(linkTo(methodOn(RootController.class).root()).withSelfRel());
        responseEntity.add(linkTo(methodOn(ImplementationController.class).getImplementations(null)).withRel(
            Constants.IMPLEMENTATIONS));
        responseEntity.add(linkTo(methodOn(SdkController.class).getSdks()).withRel(Constants.SDKS));
        responseEntity.add(
            linkTo(methodOn(RootController.class).getSelectionParams(null)).withRel(Constants.SELECTION_PARAMS));
        responseEntity.add(
            linkTo(methodOn(RootController.class).selectImplementations(null)).withRel(Constants.SELECTION));
        responseEntity.add(linkTo(methodOn(RootController.class).selectCompilerForFile(null, null)).withRel(
            Constants.COMPILER_SELECTION));
        responseEntity.add(
            linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisResults()).withRel(
                Constants.COMPILER_RESULTS));
        responseEntity.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResults(null)).withRel(
            Constants.EXECUTION_RESULTS));
        responseEntity.add(linkTo(methodOn(XmcdaCriteriaController.class).getSupportedPrioritizationMethods()).withRel(
            Constants.MCDA_METHODS));

        return new ResponseEntity<>(responseEntity, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400", content = @Content)}, description = "Retrieve selection parameters")
    @GetMapping("/" + Constants.SELECTION_PARAMS)
    public HttpEntity<ParameterListDto> getSelectionParams(@RequestParam UUID algoId) {
        LOG.debug("Get to retrieve selection parameters for algorithm with Id {} received.", algoId);

        if (Objects.isNull(algoId)) {
            LOG.error("AlgoId have to be provided to get selection parameters!");
            return new ResponseEntity("AlgoId have to be provided to get selection parameters!",
                HttpStatus.BAD_REQUEST);
        }

        // determine and return required selection parameters
        List<ParameterDto> requiredParamsDto =
            nisqAnalyzerService.getRequiredSelectionParameters(algoId).stream().map(ParameterDto.Converter::convert)
                .collect(Collectors.toList());
        ParameterListDto dto = new ParameterListDto(requiredParamsDto);

        // add required links
        dto.add(linkTo(methodOn(RootController.class).getSelectionParams(algoId)).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Select implementations for an " +
        "algorithm")
    @PostMapping("/" + Constants.SELECTION)
    public HttpEntity<AnalysisJobDto> selectImplementations(@RequestBody SelectionRequestDto params) {
        LOG.debug("Post to select implementations for algorithm with Id {} received.", params.getAlgorithmId());

        if (Objects.isNull(params.getAlgorithmId())) {
            LOG.error("Algorithm Id for the selection is null.");
            return new ResponseEntity("Algorithm Id for the selection is null.", HttpStatus.BAD_REQUEST);
        }

        if (Objects.isNull(params.getParameters())) {
            LOG.error("Parameter set for the selection is null.");
            return new ResponseEntity("Parameter set for the selection is null.", HttpStatus.BAD_REQUEST);
        }
        LOG.debug("Received {} parameters for the selection.", params.getParameters().size());

        AnalysisJob job = new AnalysisJob();
        job.setImplementedAlgorithm(params.getAlgorithmId());
        job.setTime(OffsetDateTime.now());
        String token = params.getParameters().get("token");
        params.getParameters().remove("token");
        job.setInputParameters(params.getParameters());
        params.getParameters().put("token", token);
        analysisJobRepository.save(job);

        new Thread(() -> {
            nisqAnalyzerService.performSelection(job, params.getAlgorithmId(), params.getParameters(),
                params.getRefreshToken(), params.getAllowedProviders(), params.getCompilers());
        }).start();

        AnalysisJobDto dto = AnalysisJobDto.Converter.convert(job);
        dto.add(linkTo(methodOn(AnalysisResultController.class).getAnalysisJob(job.getId())).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Select the most suitable quantum " +
        "computer for a quantum circuit passed in as file")
    @PostMapping(value = "/" + Constants.QPU_SELECTION, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HttpEntity<QpuSelectionJobDto> selectQpuForCircuitFile(@RequestBody QpuSelectionDto qpuSelectionDto,
                                                                  @RequestParam("circuit") MultipartFile circuitCode) {
        LOG.debug("Post to select QPU for given quantum circuit with language: {}",
            qpuSelectionDto.getCircuitLanguage());

        // get temp file for passed circuit code
        File circuitFile = Utils.getFileObjectFromMultipart(circuitCode);
        if (Objects.isNull(circuitFile)) {
            return new ResponseEntity("Unable to parse file from given data", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // create object for the QPU selection job and call asynchronously to update the job
        QpuSelectionJob job = new QpuSelectionJob();
        job.setTime(OffsetDateTime.now());
        job.setUserId(qpuSelectionDto.getUserId());

        if (qpuSelectionDto.getCircuitName() == null) {
            job.setCircuitName("temp");
        } else {
            job.setCircuitName(qpuSelectionDto.getCircuitName());
        }

        qpuSelectionJobRepository.save(job);
        new Thread(() -> {
            nisqAnalyzerService.performQpuSelectionForCircuit(job, qpuSelectionDto.getAllowedProviders(),
                qpuSelectionDto.getCircuitLanguage(), circuitFile, qpuSelectionDto.getTokens(),
                qpuSelectionDto.getCircuitName(), qpuSelectionDto.getCompilers(),
                qpuSelectionDto.isPreciseResultsPreference(), qpuSelectionDto.isShortWaitingTimesPreference(),
                qpuSelectionDto.getQueueImportanceRatio(), qpuSelectionDto.getMaxNumberOfCompiledCircuits(),
                qpuSelectionDto.getPredictionAlgorithm(), qpuSelectionDto.getMetaOptimizer());
        }).start();

        // send back QPU selection job to track the progress
        QpuSelectionJobDto dto = QpuSelectionJobDto.Converter.convert(job);
        dto.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJob(job.getId(),
            job.getUserId())).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    private File createQasmFileFromUrlOrString(URL url, String qasm, String refreshToken)
        throws IOException, IllegalArgumentException {
        if (Objects.isNull(url) == Objects.isNull(qasm)) {
            throw new IllegalArgumentException("Either circuitUrl or qasmCode needs to be specified.");
        }

        File circuitFile;

        // create circuit file from string or URL
        if (Objects.isNull(url)) {
            circuitFile =
                Utils.inputStreamToFile(new ByteArrayInputStream(qasm.getBytes(StandardCharsets.UTF_8)), "qasm");
        } else {
            // get file from passed URL
            circuitFile = Utils.getFileObjectFromUrl(url, refreshToken);
            if (Objects.isNull(circuitFile)) {
                throw new IOException("Unable to load file from given URL");
            }
        }

        return circuitFile;
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Select the most suitable quantum " +
        "computer for a quantum circuit loaded from the given URL")
    @PostMapping(value = "/" + Constants.QPU_SELECTION, consumes = {MediaType.APPLICATION_XML_VALUE,
        MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<QpuSelectionJobDto> selectQpuForCircuitUrl(@RequestBody QpuSelectionDto params) {
        LOG.debug("Post to select QPU for quantum circuit at URL '{}', with language '{}', and allowed providers '{}'!",
            params.getCircuitUrl(), params.getCircuitLanguage(), params.getAllowedProviders());

        File circuitFile;

        try {
            circuitFile =
                createQasmFileFromUrlOrString(params.getCircuitUrl(), params.getQasmCode(), params.getRefreshToken());
        } catch (Exception e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // create object for the QPU selection job and call asynchronously to update the job
        QpuSelectionJob job = new QpuSelectionJob();
        job.setTime(OffsetDateTime.now());
        job.setUserId(params.getUserId());

        if (params.getCircuitName() == null) {
            job.setCircuitName("temp");
        } else {
            job.setCircuitName(params.getCircuitName());
        }

        qpuSelectionJobRepository.save(job);

        new Thread(() -> {
            nisqAnalyzerService.performQpuSelectionForCircuit(job, params.getAllowedProviders(),
                params.getCircuitLanguage(), circuitFile, params.getTokens(), params.getCircuitName(),
                params.getCompilers(), params.isPreciseResultsPreference(), params.isShortWaitingTimesPreference(),
                params.getQueueImportanceRatio(), params.getMaxNumberOfCompiledCircuits(),
                params.getPredictionAlgorithm(), params.getMetaOptimizer());
        }).start();

        // send back QPU selection job to track the progress
        QpuSelectionJobDto dto = QpuSelectionJobDto.Converter.convert(job);
        dto.add(linkTo(methodOn(QpuSelectionResultController.class).getQpuSelectionJob(job.getId(),
            job.getUserId())).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Select the most suitable compiler for" +
        " an implementation passed in as file")
    @PostMapping(value = "/" + Constants.COMPILER_SELECTION, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HttpEntity<CompilationJobDto> selectCompilerForFile(@RequestBody CompilerSelectionDto compilerSelectionDto,
                                                               @RequestParam("circuit") MultipartFile circuitCode) {

        // get temp file for passed circuit code
        File circuitFile = Utils.getFileObjectFromMultipart(circuitCode);
        if (Objects.isNull(circuitFile)) {
            return new ResponseEntity("Unable to parse file from given data", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // create object for the compilation job and call asynchronously to update the job
        CompilationJob job = compilationJobRepository.save(new CompilationJob());
        new Thread(() -> {
            nisqAnalyzerService.performCompilerSelection(job, compilerSelectionDto.getProviderName().toLowerCase(),
                compilerSelectionDto.getQpuName().toLowerCase(),
                compilerSelectionDto.getCircuitLanguage().toLowerCase(), circuitFile,
                compilerSelectionDto.getCircuitName(), null, compilerSelectionDto.getTokens());
        }).start();

        // send back compilation job
        CompilationJobDto dto = CompilationJobDto.Converter.convert(job);
        dto.add(
            linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisJob(job.getId())).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", content = @Content),
        @ApiResponse(responseCode = "500", content = @Content)}, description = "Select the most suitable compiler for" +
        " an implementation loaded from the given URL")
    @PostMapping(value = "/" + Constants.COMPILER_SELECTION, consumes = {MediaType.APPLICATION_XML_VALUE,
        MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<CompilationJobDto> selectCompilerForUrl(@RequestBody CompilerSelectionDto compilerSelectionDto) {

        if (Objects.isNull(compilerSelectionDto.getProviderName()) ||
            Objects.isNull(compilerSelectionDto.getQpuName()) ||
            Objects.isNull(compilerSelectionDto.getCircuitLanguage()) ||
            Objects.isNull(compilerSelectionDto.getTokens())) {
            return new ResponseEntity("Provider name, QPU name, circuit language, and access token have to be passed!",
                HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File circuitFile;

        try {
            circuitFile =
                createQasmFileFromUrlOrString(compilerSelectionDto.getCircuitUrl(), compilerSelectionDto.getQasmCode(),
                    compilerSelectionDto.getRefreshToken());
        } catch (Exception e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // create object for the compilation job and call asynchronously to update the job
        CompilationJob job = compilationJobRepository.save(new CompilationJob());
        new Thread(() -> {
            nisqAnalyzerService.performCompilerSelection(job, compilerSelectionDto.getProviderName().toLowerCase(),
                compilerSelectionDto.getQpuName().toLowerCase(),
                compilerSelectionDto.getCircuitLanguage().toLowerCase(), circuitFile,
                compilerSelectionDto.getCircuitName(), null, compilerSelectionDto.getTokens());
        }).start();

        // send back compilation job
        CompilationJobDto dto = CompilationJobDto.Converter.convert(job);
        dto.add(
            linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisJob(job.getId())).withSelfRel());
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "400", content = @Content)}, description = "Retrieve compilers")
    @GetMapping("/" + Constants.COMPILERS)
    public HttpEntity<List<String>> getCompilers(@RequestParam String provider) {
        LOG.debug("Get to retrieve compilers for provider {} received.", provider);

        if (Objects.isNull(provider)) {
            LOG.error("Provider have to be provided to get compilers!");
            return new ResponseEntity("Provider have to be provided to get compilers!", HttpStatus.BAD_REQUEST);
        }

        // determine and return required selection parameters
        List<String> compilers = nisqAnalyzerService.getCompilers(provider);

        return new ResponseEntity<>(compilers, HttpStatus.OK);
    }
}
