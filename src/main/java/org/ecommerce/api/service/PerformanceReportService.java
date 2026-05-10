package org.ecommerce.api.service;

import org.ecommerce.api.dto.PerformanceReportDto;

import java.util.List;
import java.util.Map;

public interface PerformanceReportService {

    PerformanceReportDto.PerformanceBaselineReport generateReport();

    List<PerformanceReportDto.IdentifiedBottleneck> getBottlenecks();

    Map<String, PerformanceReportDto.CacheStatsSummary> captureCacheStats();

    PerformanceReportDto.ThroughputSnapshot getThroughputSnapshot();
}
