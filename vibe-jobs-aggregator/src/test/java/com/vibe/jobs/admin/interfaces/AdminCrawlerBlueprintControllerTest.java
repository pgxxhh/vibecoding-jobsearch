package com.vibe.jobs.admin.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.admin.application.AdminCrawlerBlueprintService;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.crawler.application.generation.CrawlerBlueprintGenerationManager.GenerationLaunchResult;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintDraft;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintGenerationTask;
import com.vibe.jobs.crawler.domain.CrawlerBlueprintStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCrawlerBlueprintControllerTest {

    @Mock
    private AdminCrawlerBlueprintService blueprintService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AdminCrawlerBlueprintController controller = new AdminCrawlerBlueprintController(blueprintService, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AdminPrincipalArgumentResolver())
                .build();
    }

    @Test
    void createAcceptsGenerationPayload() throws Exception {
        CrawlerBlueprintDraft draft = new CrawlerBlueprintDraft(
                "jd-tech",
                "京东技术岗位",
                "https://careers.jd.com/jobs",
                4,
                false,
                "",
                "",
                "",
                "",
                CrawlerBlueprintStatus.DRAFT,
                true,
                "ops@vibe.jobs",
                Instant.parse("2024-10-20T10:00:00Z"),
                Instant.parse("2024-10-20T10:00:00Z"),
                Instant.parse("2024-10-20T10:00:00Z")
        );
        CrawlerBlueprintGenerationTask task = CrawlerBlueprintGenerationTask.create("jd-tech", Map.of("entryUrl", draft.entryUrl()));
        GenerationLaunchResult result = new GenerationLaunchResult(draft, task);
        when(blueprintService.launchGeneration(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(result);

        String payload = "{" +
                "\"code\":\"JD-Tech\"," +
                "\"name\":\"京东技术岗位\"," +
                "\"entryUrl\":\"https://careers.jd.com/jobs\"," +
                "\"searchKeywords\":\"Java\"," +
                "\"excludeSelectors\":[\".banner\",\".modal\"]," +
                "\"notes\":\"需要跳过弹窗\"" +
                "}";

        mockMvc.perform(post("/admin/crawler-blueprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .requestAttr(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR, new AdminPrincipal("ops@vibe.jobs")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.summary.code").value("jd-tech"))
                .andExpect(jsonPath("$.recentTasks[0].status").value("PENDING"));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> entryUrlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keywordsCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> excludesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> notesCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);

        verify(blueprintService).launchGeneration(
                codeCaptor.capture(),
                nameCaptor.capture(),
                entryUrlCaptor.capture(),
                keywordsCaptor.capture(),
                excludesCaptor.capture(),
                notesCaptor.capture(),
                operatorCaptor.capture()
        );

        assertThat(codeCaptor.getValue()).isEqualTo("JD-Tech");
        assertThat(nameCaptor.getValue()).isEqualTo("京东技术岗位");
        assertThat(entryUrlCaptor.getValue()).isEqualTo("https://careers.jd.com/jobs");
        assertThat(keywordsCaptor.getValue()).isEqualTo("Java");
        assertThat(excludesCaptor.getValue()).containsExactlyInAnyOrder(".banner", ".modal");
        assertThat(notesCaptor.getValue()).isEqualTo("需要跳过弹窗");
        assertThat(operatorCaptor.getValue()).isEqualTo("ops@vibe.jobs");
    }
}
