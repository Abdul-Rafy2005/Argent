package com.argent.module.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatementLineResponse(
    LocalDateTime date,
    String type,
    String description,
    BigDecimal amount,
    String status
) {}
