package com.vibe.jobs.resume.application;

import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.JobDetail;
import com.vibe.jobs.resume.domain.ResumeProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;

@Component
public class RagExplanationService {

    public String buildExplanation(ResumeProfile profile, Job job, JobDetail jobDetail, List<String> skillHits) {
        StringJoiner joiner = new StringJoiner("; ");
        if (!skillHits.isEmpty()) {
            joiner.add("匹配技能: " + String.join(", ", skillHits));
        }
        if (profile.getSummary() != null && !profile.getSummary().isBlank()) {
            joiner.add("简历摘要引用: " + truncate(profile.getSummary(), 120));
        }
        if (jobDetail != null && jobDetail.getContentText() != null && !jobDetail.getContentText().isBlank()) {
            joiner.add("职位要求片段: " + truncate(jobDetail.getContentText(), 120));
        }
        if (joiner.length() == 0) {
            joiner.add("基于岗位标题与简历关键词的相似度推荐");
        }
        return joiner.toString();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
