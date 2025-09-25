
package com.vibe.jobs.service;

import com.vibe.jobs.domain.Job;
import com.vibe.jobs.repo.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class JobService {
    private final JobRepository repo;
    public JobService(JobRepository repo){ this.repo = repo; }

    @Transactional
    public Job upsert(Job incoming){
        Job existing = repo.findBySourceAndExternalId(incoming.getSource(), incoming.getExternalId());
        String checksum = checksum(incoming);
        if(existing == null){
            incoming.setChecksum(checksum);
            return repo.save(incoming);
        } else {
            if(!checksum.equals(existing.getChecksum())){
                existing.setTitle(incoming.getTitle());
                existing.setCompany(incoming.getCompany());
                existing.setLocation(incoming.getLocation());
                existing.setLevel(incoming.getLevel());
                existing.setPostedAt(incoming.getPostedAt());
                existing.setTags(incoming.getTags());
                existing.setUrl(incoming.getUrl());
                existing.setChecksum(checksum);
            }
            return existing;
        }
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
}
