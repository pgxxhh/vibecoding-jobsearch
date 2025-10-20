package com.vibe.jobs.admin.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.admin.application.AdminCrawlerBlueprintService;
import com.vibe.jobs.admin.application.AdminCrawlerBlueprintService.ActivationResult;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.admin.web.dto.CrawlerBlueprintActivationRequest;
import com.vibe.jobs.admin.web.dto.CrawlerBlueprintActivationResponse;
import com.vibe.jobs.admin.web.dto.CrawlerBlueprintDetailResponse;
import com.vibe.jobs.admin.web.dto.CrawlerBlueprintGenerationRequest;
import com.vibe.jobs.admin.web.dto.CrawlerBlueprintSummaryResponse;
import com.vibe.jobs.admin.web.dto.CrawlerBlueprintTaskResponse;
import com.vibe.jobs.admin.web.dto.DataSourceResponse;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintDraft;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintGenerationTask;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/admin/crawler-blueprints", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminCrawlerBlueprintController {

    private final AdminCrawlerBlueprintService blueprintService;
    private final ObjectMapper objectMapper;

    public AdminCrawlerBlueprintController(AdminCrawlerBlueprintService blueprintService,
                                           ObjectMapper objectMapper) {
        this.blueprintService = blueprintService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CrawlerBlueprintDetailResponse create(@RequestBody CrawlerBlueprintGenerationRequest request,
                                                 AdminPrincipal principal) {
        if (request.entryUrl() == null || request.entryUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entryUrl is required");
        }
        String operator = principal == null ? null : principal.email();
        var result = blueprintService.launchGeneration(
                request.code(),
                request.name(),
                request.entryUrl(),
                request.searchKeywords(),
                request.excludeSelectors(),
                request.notes(),
                operator
        );
        return buildDetailResponse(result.draft(), List.of(result.task()));
    }

    @PostMapping(path = "/{code}/rerun", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CrawlerBlueprintDetailResponse rerun(@PathVariable String code,
                                                @RequestBody(required = false) CrawlerBlueprintGenerationRequest request,
                                                AdminPrincipal principal) {
        CrawlerBlueprintGenerationRequest payload = request == null
                ? new CrawlerBlueprintGenerationRequest(code, null, null, null, null, null)
                : request;
        CrawlerBlueprintDraft existing = blueprintService.findDraft(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found"));
        String entryUrl = payload.entryUrl() == null || payload.entryUrl().isBlank() ? existing.entryUrl() : payload.entryUrl();
        String name = payload.name() == null || payload.name().isBlank() ? existing.name() : payload.name();
        var result = blueprintService.rerunGeneration(
                code,
                name,
                entryUrl,
                payload.searchKeywords(),
                payload.excludeSelectors(),
                payload.notes(),
                principal == null ? null : principal.email()
        );
        return buildDetailResponse(result.draft(), List.of(result.task()));
    }

    @GetMapping
    public List<CrawlerBlueprintSummaryResponse> list(@RequestParam(required = false) List<CrawlerBlueprintStatus> status,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        List<CrawlerBlueprintStatus> statuses = status == null || status.isEmpty() ? null : status;
        return blueprintService.listDrafts(statuses, page, size).stream()
                .map(CrawlerBlueprintSummaryResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{code}")
    public CrawlerBlueprintDetailResponse detail(@PathVariable String code,
                                                 @RequestParam(defaultValue = "5") int tasks) {
        CrawlerBlueprintDraft draft = blueprintService.findDraft(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found"));
        List<CrawlerBlueprintTaskResponse> recentTasks = blueprintService.listTasks(code, Math.max(1, tasks)).stream()
                .map(CrawlerBlueprintTaskResponse::fromDomain)
                .toList();
        return buildDetailResponse(draft, recentTasks);
    }

    @GetMapping("/{code}/tasks")
    public List<CrawlerBlueprintTaskResponse> tasks(@PathVariable String code,
                                                    @RequestParam(defaultValue = "10") int limit) {
        return blueprintService.listTasks(code, Math.max(1, limit)).stream()
                .map(CrawlerBlueprintTaskResponse::fromDomain)
                .toList();
    }

    @PostMapping(path = "/{code}/activate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CrawlerBlueprintActivationResponse activate(@PathVariable String code,
                                                        @RequestBody(required = false) CrawlerBlueprintActivationRequest request,
                                                        AdminPrincipal principal) {
        CrawlerBlueprintActivationRequest payload = request == null
                ? new CrawlerBlueprintActivationRequest(null, Boolean.TRUE)
                : request;
        String operator = principal == null ? null : principal.email();
        ActivationResult result = blueprintService.activateBlueprint(
                code,
                payload.dataSourceCode(),
                payload.enableOrDefault(),
                operator
        );
        return CrawlerBlueprintActivationResponse.of(
                CrawlerBlueprintSummaryResponse.fromDomain(result.draft()),
                DataSourceResponse.fromDomain(result.dataSource())
        );
    }

    private CrawlerBlueprintDetailResponse buildDetailResponse(CrawlerBlueprintDraft draft,
                                                               List<CrawlerBlueprintGenerationTask> tasks) {
        CrawlerBlueprintSummaryResponse summary = CrawlerBlueprintSummaryResponse.fromDomain(draft);
        Object report = parseJsonSilently(draft.lastTestReportJson());
        return new CrawlerBlueprintDetailResponse(
                summary,
                draft.draftConfigJson(),
                report,
                tasks.stream().map(CrawlerBlueprintTaskResponse::fromDomain).toList()
        );
    }

    private Object parseJsonSilently(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
