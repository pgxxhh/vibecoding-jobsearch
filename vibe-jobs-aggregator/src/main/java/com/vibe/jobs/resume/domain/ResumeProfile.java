package com.vibe.jobs.resume.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeProfile {
    @Builder.Default
    private String rawText = "";

    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Builder.Default
    private List<String> experiences = new ArrayList<>();

    @Builder.Default
    private String summary = "";
}
