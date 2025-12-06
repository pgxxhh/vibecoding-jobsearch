package com.vibe.jobs.resume.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resume.storage")
public class ResumeStorageProperties {
    public enum StorageBackend {
        LOCAL,
        GCS
    }

    private String bucket;
    private String prefix = "resumes";
    private String localDirectory = "./uploads/resumes";
    private StorageBackend backend = StorageBackend.LOCAL;
    private long maxFileSizeBytes = 5 * 1024 * 1024;
    private String allowedContentTypes = "application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain";

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getLocalDirectory() {
        return localDirectory;
    }

    public void setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
    }

    public StorageBackend getBackend() {
        return backend;
    }

    public void setBackend(StorageBackend backend) {
        this.backend = backend;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(String allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
