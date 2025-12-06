package com.vibe.jobs.resume.application;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.vibe.jobs.resume.config.ResumeStorageProperties;
import com.vibe.jobs.resume.config.ResumeStorageProperties.StorageBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ResumeStorageService {
    private static final Logger log = LoggerFactory.getLogger(ResumeStorageService.class);
    private final Storage storage;
    private final ResumeStorageProperties properties;
    private final Set<String> allowedContentTypes;

    public ResumeStorageService(Storage storage, ResumeStorageProperties properties) {
        this.storage = storage;
        this.properties = properties;
        this.allowedContentTypes = StringUtils.commaDelimitedListToSet(properties.getAllowedContentTypes()).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public String store(MultipartFile file) throws IOException {
        if (properties.getBackend() == StorageBackend.LOCAL) {
            log.info("Storing resume locally with filename hint: {}", file.getOriginalFilename());
            return storeLocally(file);
        }
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalStateException("Resume storage bucket is not configured");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String objectName = buildObjectName(filename);

        BlobId blobId = BlobId.of(properties.getBucket(), objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getInputStream());
        log.info("Stored resume in GCS bucket={} object={} contentType={}", properties.getBucket(), objectName, file.getContentType());
        return String.format("gs://%s/%s", properties.getBucket(), objectName);
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            log.warn("Rejected resume upload exceeding max size: {} bytes", file.getSize());
            throw new IllegalArgumentException("File too large");
        }
        String contentType = file.getContentType();
        if (contentType != null && !allowedContentTypes.isEmpty() && !allowedContentTypes.contains(contentType)) {
            log.warn("Rejected resume upload with unsupported content type: {}", contentType);
            throw new IllegalArgumentException("Unsupported file type");
        }
    }

    private String resolveExtension(String name) {
        if (name == null) {
            return "";
        }
        int idx = name.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return name.substring(idx + 1).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private String buildObjectName(String filename) {
        String prefix = properties.getPrefix();
        if (StringUtils.hasText(prefix)) {
            String normalized = prefix.replaceAll("^/+|/+$", "");
            if (!normalized.isEmpty()) {
                return normalized + "/" + filename;
            }
        }
        return filename;
    }

    private String storeLocally(MultipartFile file) throws IOException {
        if (!StringUtils.hasText(properties.getLocalDirectory())) {
            throw new IllegalStateException("Local resume storage directory is not configured");
        }
        Path directory = Paths.get(properties.getLocalDirectory());
        Files.createDirectories(directory);
        String extension = resolveExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = directory.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored resume locally at {}", target.toAbsolutePath());
        return target.toAbsolutePath().toString();
    }
}
