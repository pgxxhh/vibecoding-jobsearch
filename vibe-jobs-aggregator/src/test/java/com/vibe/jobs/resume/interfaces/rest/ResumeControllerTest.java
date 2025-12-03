package com.vibe.jobs.resume.interfaces.rest;

import com.vibe.jobs.admin.application.AdminAccessService;
import com.vibe.jobs.auth.application.EmailAuthService;
import com.vibe.jobs.resume.application.ResumeFeedbackService;
import com.vibe.jobs.resume.application.ResumeRecommendationRequest;
import com.vibe.jobs.resume.application.ResumeRecommendationService;
import com.vibe.jobs.resume.application.ResumeService;
import com.vibe.jobs.resume.domain.Resume;
import com.vibe.jobs.resume.domain.ResumeParseStatus;
import com.vibe.jobs.resume.domain.ResumeRecommendation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeService resumeService;

    @MockBean
    private ResumeRecommendationService resumeRecommendationService;

    @MockBean
    private ResumeFeedbackService resumeFeedbackService;

    @MockBean
    private EmailAuthService emailAuthService;

    @MockBean
    private AdminAccessService adminAccessService;

    @Test
    void uploadReturnsResumeIdAndStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "java developer".getBytes());
        when(resumeService.upload(any(), any())).thenReturn(Resume.builder().id(5L).parseStatus(ResumeParseStatus.READY).build());

        mockMvc.perform(multipart("/resumes/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeId").value(5))
                .andExpect(jsonPath("$.parseStatus").value("READY"));
    }

    @Test
    void recommendDelegatesToService() throws Exception {
        when(resumeRecommendationService.recommend(any(ResumeRecommendationRequest.class))).thenAnswer(invocation -> {
            ResumeRecommendationRequest captured = invocation.getArgument(0);
            assertThat(captured.getResumeId()).isEqualTo(9L);
            assertThat(captured.getLocation()).isEqualTo("remote");
            return List.of(ResumeRecommendation.builder()
                    .jobId(1L)
                    .title("Backend Engineer")
                    .company("Acme")
                    .location("Remote")
                    .score(0.9)
                    .skillHits(List.of("java"))
                    .explanation("match")
                    .build());
        });

        mockMvc.perform(get("/resumes/9/recommendations").param("location", "remote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].jobId").value(1))
                .andExpect(jsonPath("$.items[0].explanation").value("match"));
    }
}
