package com.vibe.jobs.companydiscovery.infrastructure.provider;

import com.vibe.jobs.companydiscovery.domain.CompanyCandidate;
import com.vibe.jobs.companydiscovery.domain.spi.CompanyDiscoveryProviderPort;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmartRecruitersCompanyDiscoveryAdapterTest {

    private MockWebServer server;
    private SmartRecruitersCompanyDiscoveryAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        adapter = new SmartRecruitersCompanyDiscoveryAdapter(WebClient.builder());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void discoverParsesCompanyList() {
        server.enqueue(new MockResponse()
                .setBody("{\"content\":[{\"identifier\":\"acme\",\"name\":\"Acme Corp\"}]}")
                .addHeader("Content-Type", "application/json"));

        CompanyDiscoveryProviderPort.CompanyDiscoveryRequest request =
                new CompanyDiscoveryProviderPort.CompanyDiscoveryRequest(
                        "smartrecruiters",
                        5,
                        server.url("/companies").toString(),
                        List.of()
                );

        List<CompanyCandidate> candidates = adapter.discover(request);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).reference()).isEqualTo("acme");
        assertThat(candidates.get(0).displayName()).isEqualTo("Acme Corp");
    }
}
