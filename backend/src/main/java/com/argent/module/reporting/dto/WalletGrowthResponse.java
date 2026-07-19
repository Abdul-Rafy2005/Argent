package com.argent.module.reporting.dto;

import java.time.LocalDate;

public record WalletGrowthResponse(
    LocalDate date,
    long walletsCreated
) {}
