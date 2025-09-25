
package com.vibe.jobs.sources;
import com.vibe.jobs.domain.Job;
import java.util.List;
public interface SourceClient {
    String sourceName();
    List<Job> fetchPage(int page, int size) throws Exception;
    default boolean supportsIncremental(){ return false; }
}
