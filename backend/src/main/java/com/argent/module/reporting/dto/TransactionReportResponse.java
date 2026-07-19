package com.argent.module.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionReportResponse(
    java.util.UUID id,
    String type,
    String status,
    BigDecimal amount,
    String environment,
    LocalDateTime createdAt
) {}
