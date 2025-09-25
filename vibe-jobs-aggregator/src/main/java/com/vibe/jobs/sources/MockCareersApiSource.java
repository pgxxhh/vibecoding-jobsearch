
package com.vibe.jobs.sources;
import com.vibe.jobs.domain.Job;
import java.time.Instant;
import java.util.*;
public class MockCareersApiSource implements SourceClient {
    private final String company;
    public MockCareersApiSource(String company){ this.company = company; }
    @Override public String sourceName(){ return "api:" + company.toLowerCase(); }
    @Override public List<Job> fetchPage(int page, int size){
        if(page>1) return List.of();
        List<Job> list = new ArrayList<>();
        list.add(Job.builder().source(sourceName()).externalId("REQ-1001").title("Senior Backend Engineer (Java)")
                .company(company).location("Tokyo, JP").level("Senior")
                .postedAt(Instant.now().minusSeconds(3600*24*2))
                .tags(new HashSet<>(List.of("Java","Spring Boot","Microservices")))
                .url("https://careers."+company.toLowerCase()+".com/jobs/1001").build());
        return list;
    }
}
