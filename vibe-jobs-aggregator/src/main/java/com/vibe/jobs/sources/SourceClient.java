
package com.vibe.jobs.sources;
import java.util.List;

public interface SourceClient {
    String sourceName();
    List<FetchedJob> fetchPage(int page, int size) throws Exception;
    default boolean supportsIncremental(){ return false; }
}
