
package com.vibe.jobs.jobposting.application;

import com.vibe.jobs.jobposting.domain.Job;
import com.vibe.jobs.jobposting.domain.spi.JobRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class JobService {
    private final JobRepositoryPort repo;

    public JobService(JobRepositoryPort repo){ this.repo = repo; }

    @Transactional
    public Job upsert(Job incoming){
        String checksum = checksum(incoming);
        Optional<Job> existingOpt = repo.findBySourceAndExternalId(incoming.getSource(), incoming.getExternalId());
        if(existingOpt.isPresent()){
            Job existing = existingOpt.get();
            if(!checksum.equals(existing.getChecksum())){
                applyUpdates(existing, incoming, checksum);
                existing = repo.save(existing);
            }
            return existing;
        }

        if(hasCompanyAndTitle(incoming)){
            Optional<Job> duplicate = repo.findMostRecentByCompanyAndTitleIgnoreCase(
                    incoming.getCompany(), incoming.getTitle());
            if(duplicate.isPresent()){
                Job current = duplicate.get();
                if(!checksum.equals(current.getChecksum())){
                    applyUpdates(current, incoming, checksum);
                    current = repo.save(current);
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
