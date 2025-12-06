package com.vibe.jobs.resume.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.jobs.resume.domain.Resume;
import com.vibe.jobs.resume.domain.ResumeParseStatus;
import com.vibe.jobs.resume.domain.ResumeProfile;
import com.vibe.jobs.resume.domain.spi.ResumeRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    private final ResumeRepositoryPort resumeRepositoryPort;
    private final ResumeStorageService storageService;
    private final ResumeParsingService parsingService;
    private final ObjectMapper objectMapper;

    public ResumeService(ResumeRepositoryPort resumeRepositoryPort,
                         ResumeStorageService storageService,
                         ResumeParsingService parsingService,
                         ObjectMapper objectMapper) {
        this.resumeRepositoryPort = resumeRepositoryPort;
        this.storageService = storageService;
        this.parsingService = parsingService;
        this.objectMapper = objectMapper;
    }

    public Resume upload(MultipartFile file, Long userId) throws IOException {
        storageService.validate(file);
        String storedPath = storageService.store(file);
        log.info("Stored resume for userId={} at path={} backend={}", userId, storedPath, storageService.getClass().getSimpleName());
        Resume resume = Resume.builder()
                .userId(userId)
                .originalFilename(file.getOriginalFilename())
                .filePath(storedPath)
                .parseStatus(ResumeParseStatus.PENDING)
                .build();
        Resume saved = resumeRepositoryPort.save(resume);
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("text")) {
                log.warn("Skip parsing for resume {} due to non-text contentType={} (expect text/*)", saved.getId(), contentType);
                saved.setParseStatus(ResumeParseStatus.FAILED);
                return resumeRepositoryPort.save(saved);
            }
            ResumeProfile profile = parsingService.parse(file.getBytes());
            String profileJson = objectMapper.writeValueAsString(profile);
            saved.setParsedJson(profileJson);
            saved.setLanguage(parsingService.detectLanguage(profile.getRawText()));
            saved.setParseStatus(ResumeParseStatus.READY);
            log.info("Parsed resume {} successfully with detected language {}", saved.getId(), saved.getLanguage());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize parsed resume profile for resume {}", saved.getId(), e);
            saved.setParseStatus(ResumeParseStatus.FAILED);
        }
        return resumeRepositoryPort.save(saved);
    }

    public ResumeProfile readProfile(Resume resume) {
        if (resume.getParsedJson() == null || resume.getParsedJson().isBlank()) {
            log.warn("Resume {} has no parsed profile", resume.getId());
            return ResumeProfile.builder().build();
        }
        try {
            return objectMapper.readValue(resume.getParsedJson(), ResumeProfile.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize profile JSON for resume {}", resume.getId(), ex);
            return ResumeProfile.builder().build();
        }
    }
}
