package com.argent.module.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyVolumeResponse(
    LocalDate date,
    long transactionCount,
    BigDecimal totalAmount
) {}
