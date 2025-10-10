
package com.vibe.jobs.web;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.web.dto.JobDto;
import java.util.ArrayList;
public class JobMapper {
    public static JobDto toDto(Job j) {
        return toDto(j, false);
    }

    public static JobDto toDto(Job j, boolean detailMatch) {
        return new JobDto(
                String.valueOf(j.getId()),
                j.getTitle(),
                j.getCompany(),
                j.getLocation(),
                j.getLevel(),
                j.getPostedAt(),
                new ArrayList<>(j.getTags()),
                j.getUrl(),
                detailMatch
        );
    }
}
