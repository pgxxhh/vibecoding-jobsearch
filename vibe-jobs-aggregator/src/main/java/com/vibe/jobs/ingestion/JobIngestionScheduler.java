package com.vibe.jobs.ingestion;
import com.vibe.jobs.domain.Job;
import com.vibe.jobs.service.JobService;
import com.vibe.jobs.sources.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
@Slf4j @Component
public class JobIngestionScheduler {
    private final JobService jobService;
    @Value("${ingestion.pageSize:100}") private int pageSize;
    public JobIngestionScheduler(JobService jobService){ this.jobService = jobService; }

    private List<SourceClient> sources(){
        return Arrays.asList(
            new MockCareersApiSource("Acme"),
            new ArbeitnowSourceClient(),
            new GreenhouseSourceClient("stripe"),
            new StandardCareersApiSourceClient(
                "Microsoft",
                "https://careers.microsoft.com/api"
            )
        );
    }

    @Scheduled(fixedDelayString = "${ingestion.fixedDelayMs:3600000}", initialDelayString = "${ingestion.initialDelayMs:10000}")
    public void runIngestion(){
        for(SourceClient s: sources()){
            try{
                int page=1;
                while(true){
                    var items = s.fetchPage(page, pageSize);
                    if(items==null || items.isEmpty()) break;
                    for(Job j: items) jobService.upsert(j);
                    page++;
                }
            }catch(Exception e){ /* log omitted */ }
        }
    }
}
