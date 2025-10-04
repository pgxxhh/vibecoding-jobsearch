package com.vibe.jobs.admin.web.dto;

import java.util.List;

public record BulkDataSourceRequest(
        List<DataSourceRequest> dataSources
) {
}