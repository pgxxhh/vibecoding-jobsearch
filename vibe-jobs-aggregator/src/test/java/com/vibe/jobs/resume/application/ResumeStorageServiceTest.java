package com.vibe.jobs.resume.application;

import com.google.cloud.storage.Storage;
import com.vibe.jobs.resume.config.ResumeStorageProperties;
import com.vibe.jobs.resume.config.ResumeStorageProperties.StorageBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ResumeStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesFileLocallyWhenBackendIsLocal() throws IOException {
        ResumeStorageProperties properties = new ResumeStorageProperties();
        properties.setBackend(StorageBackend.LOCAL);
        properties.setLocalDirectory(tempDir.resolve("resumes").toString());

        ResumeStorageService service = new ResumeStorageService(mock(Storage.class), properties);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "hello world".getBytes()
        );

        String storedPath = service.store(file);

        assertThat(storedPath).contains(tempDir.toString());
        assertThat(Files.readString(Path.of(storedPath))).isEqualTo("hello world");
    }
}
