package com.argent.module.wallet.dto;

import java.util.Map;

public record UpdateWalletRequest(
    String label,
    Map<String, Object> metadata
) {}
