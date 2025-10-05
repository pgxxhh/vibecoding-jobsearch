
package com.vibe.jobs.sources;

import com.vibe.jobs.ingestion.domain.IngestionCursor;

import java.util.List;

public interface SourceClient {
    String sourceName();
    List<FetchedJob> fetchPage(int page, int size) throws Exception;

    default boolean supportsIncremental() {
        return false;
    }

    default FetchResult fetchSince(IngestionCursor cursor, int pageSize) throws Exception {
        List<FetchedJob> jobs = fetchPage(1, pageSize);
        boolean hasMore = jobs != null && !jobs.isEmpty();
        return new FetchResult(jobs, null, hasMore);
    }

    record FetchResult(List<FetchedJob> jobs, String nextPageToken, boolean hasMore) {
        public FetchResult {
            jobs = jobs == null ? List.of() : List.copyOf(jobs);
        }

        public static FetchResult empty() {
            return new FetchResult(List.of(), null, false);
        }
    }
}
