
package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.infrastructure.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class JobService {
    private final JobRepository repo;
    public JobService(JobRepository repo){ this.repo = repo; }

    @Transactional
    public Job upsert(Job incoming){
        Job existing = repo.findBySourceAndExternalId(incoming.getSource(), incoming.getExternalId());
        String checksum = checksum(incoming);
        if(existing != null){
            if(!checksum.equals(existing.getChecksum())){
                applyUpdates(existing, incoming, checksum);
            }
            return existing;
        }

        if(hasCompanyAndTitle(incoming)){
            Optional<Job> duplicate = repo.findTopByCompanyIgnoreCaseAndTitleIgnoreCase(
                    incoming.getCompany(), incoming.getTitle());
            if(duplicate.isPresent()){
                Job current = duplicate.get();
                if(!checksum.equals(current.getChecksum())){
                    applyUpdates(current, incoming, checksum);
                }
                return current;
            }
        }

        incoming.setChecksum(checksum);
        return repo.save(incoming);
    }

    private String checksum(Job j){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String s = String.join("|",
                    safe(j.getTitle()), safe(j.getCompany()), safe(j.getLocation()), safe(j.getLevel()),
                    j.getPostedAt()==null?"":j.getPostedAt().toString(),
                    String.join(",", j.getTags()), safe(j.getUrl())
            );
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        }catch(Exception e){ throw new RuntimeException(e); }
    }
    private String safe(String s){ return s==null?"":s; }

    private boolean hasCompanyAndTitle(Job job){
        return !isBlank(job.getCompany()) && !isBlank(job.getTitle());
    }

    private boolean isBlank(String value){
        return value == null || value.trim().isEmpty();
    }

    private void applyUpdates(Job target, Job source, String checksum){
        target.setTitle(source.getTitle());
        target.setCompany(source.getCompany());
        target.setLocation(source.getLocation());
        target.setLevel(source.getLevel());
        target.setPostedAt(source.getPostedAt());
        target.setTags(source.getTags());
        target.setUrl(source.getUrl());
        target.setChecksum(checksum);
    }
}
