package com.vibe.jobs.datasource.application.migration;

import com.vibe.jobs.config.IngestionProperties;
import com.vibe.jobs.datasource.application.DataSourceCommandService;
import com.vibe.jobs.datasource.domain.JobDataSource;
import com.vibe.jobs.datasource.domain.JobDataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyIngestionConfigurationImporterTest {

    @Mock
    private DataSourceCommandService commandService;

    @Mock
    private JobDataSourceRepository repository;

    @Captor
    private ArgumentCaptor<List<JobDataSource>> dataSourceListCaptor;

    private IngestionProperties properties;

    private LegacyIngestionConfigurationImporter importer;

    @BeforeEach
    void setUp() {
        properties = new IngestionProperties();
        importer = new LegacyIngestionConfigurationImporter(properties, commandService, repository);
    }

    @Test
    void migratesAllSourcesWhenDatabaseIsEmpty() {
        properties.setCompanies(List.of("Acme"));
        IngestionProperties.Source ashby = new IngestionProperties.Source();
        ashby.setId("ashby");
        ashby.setType("ashby");
        ashby.setRequireOverride(false);
        ashby.setOptions(Map.of("token", "abc"));
        properties.setSources(List.of(ashby));

        when(repository.existsAny()).thenReturn(false);
        when(repository.findByCode("ashby")).thenReturn(Optional.empty());

        importer.migrateIfNecessary();

        verify(commandService).saveAll(dataSourceListCaptor.capture());
        assertThat(dataSourceListCaptor.getValue()).hasSize(1);
        JobDataSource saved = dataSourceListCaptor.getValue().get(0);
        assertThat(saved.getCode()).isEqualTo("ashby");
        assertThat(saved.getCompanies()).hasSize(1);
        assertThat(saved.getCompanies().get(0).reference()).isEqualTo("acme");
    }

    @Test
    void updatesExistingSourceWhenConfigurationChanges() {
        properties.setCompanies(List.of("Acme"));
        IngestionProperties.Source ashby = new IngestionProperties.Source();
        ashby.setId("ashby");
        ashby.setType("ashby");
        ashby.setRequireOverride(false);
        ashby.setOptions(Map.of("token", "updated"));
        properties.setSources(List.of(ashby));

        JobDataSource existing = new JobDataSource(
                10L,
                "ashby",
                "ashby",
                true,
                true,
                false,
                JobDataSource.Flow.UNLIMITED,
                Map.of("token", "old"),
                List.of(),
                List.of(new JobDataSource.DataSourceCompany(1L, "acme", "Acme", "acme", true, Map.of(), Map.of()))
        );

        when(repository.existsAny()).thenReturn(true);
        when(repository.findByCode("ashby")).thenReturn(Optional.of(existing));

        importer.migrateIfNecessary();

        ArgumentCaptor<JobDataSource> updatedCaptor = ArgumentCaptor.forClass(JobDataSource.class);
        verify(commandService).save(updatedCaptor.capture());
        JobDataSource updated = updatedCaptor.getValue();
        assertThat(updated.getId()).isEqualTo(10L);
        assertThat(updated.getBaseOptions()).containsEntry("token", "updated");
    }

    @Test
    void skipsUpdateWhenExistingConfigurationMatches() {
        properties.setCompanies(List.of("Acme"));
        IngestionProperties.Source ashby = new IngestionProperties.Source();
        ashby.setId("ashby");
        ashby.setType("ashby");
        ashby.setRequireOverride(false);
        ashby.setOptions(Map.of("token", "abc"));
        properties.setSources(List.of(ashby));

        JobDataSource existing = new JobDataSource(
                10L,
                "ashby",
                "ashby",
                true,
                true,
                false,
                JobDataSource.Flow.UNLIMITED,
                Map.of("token", "abc"),
                List.of(),
                List.of(new JobDataSource.DataSourceCompany(1L, "acme", "Acme", "acme", true, Map.of(), Map.of()))
        );

        when(repository.existsAny()).thenReturn(true);
        when(repository.findByCode("ashby")).thenReturn(Optional.of(existing));

        importer.migrateIfNecessary();

        verify(commandService, never()).save(org.mockito.ArgumentMatchers.any());
        verify(commandService, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
