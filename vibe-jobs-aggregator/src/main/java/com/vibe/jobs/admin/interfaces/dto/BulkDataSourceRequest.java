package com.vibe.jobs.admin.interfaces.dto;

import java.util.List;

public record BulkDataSourceRequest(
        List<DataSourceRequest> dataSources
) {
}