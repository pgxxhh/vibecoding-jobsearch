package com.vibe.jobs.admin.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.admin.application.AdminChangeLogService;
import com.vibe.jobs.admin.application.CompanyDiscoverySettingsService;
import com.vibe.jobs.admin.domain.AdminPrincipal;
import com.vibe.jobs.admin.domain.CompanyDiscoverySettingsSnapshot;
import com.vibe.jobs.admin.interfaces.dto.CompanyDiscoverySettingsRequest;
import com.vibe.jobs.companydiscovery.CompanyDiscoveryScheduler;
import com.vibe.jobs.companydiscovery.application.CompanyDiscoveryService;
import com.vibe.jobs.shared.infrastructure.config.CompanyDiscoveryProperties;
import com.vibe.jobs.shared.infrastructure.config.IngestionProperties;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCompanyDiscoveryControllerTest {

    @Mock
    private CompanyDiscoverySettingsService settingsService;

    @Mock
    private CompanyDiscoveryService discoveryService;

    @Mock
    private CompanyDiscoveryScheduler scheduler;

    @Mock
    private AdminChangeLogService changeLogService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AdminCompanyDiscoveryController controller = new AdminCompanyDiscoveryController(
                settingsService,
                discoveryService,
                scheduler,
                changeLogService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AdminPrincipalArgumentResolver())
                .build();
    }

    @Test
    void updateSettingsPersistsSnapshot() throws Exception {
        CompanyDiscoverySettingsSnapshot before = new CompanyDiscoverySettingsSnapshot(
                false,
                60000L,
                10000L,
                20,
                100,
                true,
                List.of("lever"),
                List.of(),
                Map.of("seed", new CompanyDiscoveryProperties.ProviderSettings()),
                new IngestionProperties.LocationFilter(),
                new IngestionProperties.RoleFilter(),
                Instant.parse("2024-10-01T00:00:00Z")
        );
        CompanyDiscoverySettingsSnapshot after = new CompanyDiscoverySettingsSnapshot(
                true,
                120000L,
                5000L,
                30,
                150,
                false,
                List.of("smartrecruiters"),
                List.of("bad-co"),
                Map.of("smartrecruiters", new CompanyDiscoveryProperties.ProviderSettings()),
                new IngestionProperties.LocationFilter(),
                new IngestionProperties.RoleFilter(),
                Instant.parse("2024-10-02T00:00:00Z")
        );
        when(settingsService.current()).thenReturn(before);
        when(settingsService.update(any())).thenReturn(after);

        CompanyDiscoverySettingsRequest request = new CompanyDiscoverySettingsRequest(
                true,
                120000L,
                5000L,
                30,
                150,
                false,
                List.of("smartrecruiters"),
                List.of("bad-co"),
                Map.of(),
                new IngestionProperties.LocationFilter(),
                new IngestionProperties.RoleFilter()
        );

        mockMvc.perform(put("/admin/company-discovery/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR, new AdminPrincipal("ops@vibe.jobs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.pageSize").value(30));

        ArgumentCaptor<CompanyDiscoverySettingsSnapshot> snapshotCaptor = ArgumentCaptor.forClass(CompanyDiscoverySettingsSnapshot.class);
        verify(settingsService).update(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().pageSize()).isEqualTo(30);

        verify(changeLogService).record(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }
}
