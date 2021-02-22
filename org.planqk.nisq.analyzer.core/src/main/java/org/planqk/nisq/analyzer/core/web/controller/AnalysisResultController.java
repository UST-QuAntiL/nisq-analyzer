package org.planqk.nisq.analyzer.core.web.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.control.NisqAnalyzerControlService;
import org.planqk.nisq.analyzer.core.model.AnalysisResult;
import org.planqk.nisq.analyzer.core.model.ExecutionResult;
import org.planqk.nisq.analyzer.core.model.Implementation;
import org.planqk.nisq.analyzer.core.model.AnalysisJob;
import org.planqk.nisq.analyzer.core.model.ParameterValue;
import org.planqk.nisq.analyzer.core.repository.AnalysisResultRepository;
import org.planqk.nisq.analyzer.core.repository.ExecutionResultRepository;
import org.planqk.nisq.analyzer.core.repository.ImplementationSelectionJobRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisResultListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ExecutionResultDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisJobDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.AnalysisJobListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Tag(name = "analysis-result")
@RestController
@CrossOrigin(allowedHeaders = "*", origins = "*")
@RequestMapping("/" + Constants.ANALYSIS_RESULTS)
public class AnalysisResultController {
    private final static Logger LOG = LoggerFactory.getLogger(AnalysisResultController.class);

    private final AnalysisResultRepository analysisResultRepository;

    private final ExecutionResultRepository executionResultRepository;

    private final ImplementationSelectionJobRepository implementationSelectionJobRepository;

    private final NisqAnalyzerControlService controlService;

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve all analysis results for an Algorithm")
    @Parameter(in = ParameterIn.QUERY
            , description = "Sorting criteria in the format: property(,asc|desc). "
            + "Default sort order is ascending. " + "Multiple sort criteria are supported."
            , name = "sort"
            , content = @Content(array = @ArraySchema(schema = @Schema(type = "string"))))
    @GetMapping("/algorithm/{algoId}")
    public HttpEntity<AnalysisResultListDto> getAnalysisResults(@PathVariable UUID algoId,
                                                                @Parameter(hidden = true) Sort sort) {
        LOG.debug("Get to retrieve all analysis results for impl with id: {}.", algoId);
        AnalysisResultListDto model = new AnalysisResultListDto();
        model.add(analysisResultRepository.findByImplementedAlgorithm(algoId, sort)
                .stream().map(this::createAnalysisResultDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(AnalysisResultController.class).getAnalysisResults(algoId, sort)).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve all compiler analysis jobs")
    @GetMapping("/" + Constants.ANALYSIS_JOBS)
    @Transactional
    public HttpEntity<AnalysisJobListDto> getImplementationSelectionJobs() {
        AnalysisJobListDto model = new AnalysisJobListDto();
        model.add(implementationSelectionJobRepository.findAll().stream().map(this::createDto).collect(Collectors.toList()));
        model.add(linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisJobs()).withSelfRel());
        return new ResponseEntity<>(model, HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve a single analysis result")
    @GetMapping("/{resId}")
    public HttpEntity<AnalysisResultDto> getAnalysisResult(@PathVariable UUID resId) {
        LOG.debug("Get to retrieve analysis result with id: {}.", resId);

        Optional<AnalysisResult> result = analysisResultRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve analysis result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createAnalysisResultDto(result.get()), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", content = @Content)},
            description = "Retrieve a single implementation selection result")
    @GetMapping("/" + Constants.ANALYSIS_JOBS + "/{resId}")
    @Transactional
    public HttpEntity<AnalysisJobDto> getImplementationSelectionJob(@PathVariable UUID resId) {
        LOG.debug("Get to retrieve implementation selection job with id: {}.", resId);

        Optional<AnalysisJob> result = implementationSelectionJobRepository.findById(resId);
        if (!result.isPresent()) {
            LOG.error("Unable to retrieve implementation selection result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(createDto(result.get()), HttpStatus.OK);
    }

    @Operation(responses = {@ApiResponse(responseCode = "202"), @ApiResponse(responseCode = "404", content = @Content),
            @ApiResponse(responseCode = "500", content = @Content)}, description = "Execute an analysis configuration")
    @PostMapping("/{resId}/" + Constants.EXECUTION)
    public HttpEntity<ExecutionResultDto> executeAnalysisResult(@PathVariable UUID resId) {
        LOG.debug("Post to execute analysis result with id: {}", resId);

        Optional<AnalysisResult> analysisResultOptional = analysisResultRepository.findById(resId);
        if (!analysisResultOptional.isPresent()) {
            LOG.error("Unable to retrieve analysis result with id {} from the repository.", resId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final AnalysisResult analysisResult = analysisResultOptional.get();

        try {
            Implementation implementation = analysisResult.getImplementation();

            // Retrieve the type of the parameter from the algorithm definition
            Map<String, ParameterValue> typedParams =
                    ParameterValue.inferTypedParameterValue(implementation.getInputParameters(), analysisResult.getInputParameters());

            ExecutionResult result = controlService.executeQuantumAlgorithmImplementation(analysisResult, typedParams);

            ExecutionResultDto dto = ExecutionResultDto.Converter.convert(result);
            dto.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResult(result.getId())).withSelfRel());
            return new ResponseEntity<>(dto, HttpStatus.ACCEPTED);
        } catch (RuntimeException e) {
            LOG.error("Error while executing implementation", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private AnalysisResultDto createAnalysisResultDto(AnalysisResult result) {
        AnalysisResultDto dto = AnalysisResultDto.Converter.convert(result);
        dto.add(linkTo(methodOn(AnalysisResultController.class)
                .getAnalysisResult(result.getId()))
                .withSelfRel());
        dto.add(linkTo(methodOn(ImplementationController.class)
                .getImplementation(result.getImplementation().getId()))
                .withRel(Constants.EXECUTED_ALGORITHM_LINK));
        for (ExecutionResult executionResult : executionResultRepository.findByAnalysisResult(result)) {
            dto.add(linkTo(methodOn(ExecutionResultController.class).getExecutionResult(executionResult.getId()))
                    .withRel(Constants.EXECUTION + "-" + executionResult.getId()));
        }
        return dto;
    }

    private AnalysisJobDto createDto(AnalysisJob job) {
        AnalysisJobDto dto = AnalysisJobDto.Converter.convert(job);
        dto.add(linkTo(methodOn(CompilerAnalysisResultController.class).getCompilerAnalysisJob(job.getId())).withSelfRel());
        return dto;
    }
}
