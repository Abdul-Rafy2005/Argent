package com.argent.module.transaction.engine;

import com.argent.module.transaction.entity.Transaction;

public interface TransactionEngine {
    Transaction.Type getSupportedType();
    void validate(Transaction transaction);
    Transaction execute(Transaction transaction);
}
