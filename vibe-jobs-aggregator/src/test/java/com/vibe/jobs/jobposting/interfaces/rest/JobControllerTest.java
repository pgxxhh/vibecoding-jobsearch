package com.vibe.jobs.jobposting.interfaces.rest;

import com.vibe.jobs.jobposting.application.JobDetailService;
import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.spi.JobRepositoryPort;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobRepositoryPort repo;

    @MockBean
    private JobDetailService jobDetailService;

    @BeforeEach
    void setUp() {
        when(jobDetailService.findByJobIds(anyCollection())).thenReturn(Map.of());
    }

    @Test
    void listWithoutIncludeTotalSkipsCounting() throws Exception {
        when(repo.searchAfter(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of(sampleJob()));

        mockMvc.perform(get("/jobs").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(Matchers.nullValue()));

        verify(repo, never()).countSearch(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void listWithIncludeTotalReturnsValue() throws Exception {
        when(repo.searchAfter(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of(sampleJob()));
        when(repo.countSearch(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(42L);

        mockMvc.perform(get("/jobs")
                        .param("size", "1")
                        .param("includeTotal", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(42));

        verify(repo).countSearch(any(), any(), any(), any(), any(), anyBoolean());
    }

    private Job sampleJob() {
        return Job.builder()
                .id(1L)
                .source("test")
                .externalId("ext-1")
                .title("Sample Job")
                .company("Acme")
                .location("Remote")
                .postedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .url("https://example.com/job")
                .build();
    }
}
