package com.argent.module.ledger.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReconciliationResponse(
    UUID accountId,
    BigDecimal ledgerBalance,
    BigDecimal storedBalance,
    boolean reconciled,
    String message
) {}
