package org.ecommerce.api.service;

import org.ecommerce.api.dto.PerformanceReportDto;

import java.util.List;

public interface PerformanceReportService {

    PerformanceReportDto.PerformanceBaselineReport generateReport();

    List<PerformanceReportDto.IdentifiedBottleneck> getBottlenecks();
}
