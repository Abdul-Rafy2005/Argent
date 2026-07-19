package com.argent.module.wallet.service;

import com.argent.module.organization.entity.Organization;
import com.argent.module.wallet.entity.Account;
import com.argent.module.wallet.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(Organization organization, Account.Type type, String name, Account.Environment environment) {
        Account account = Account.builder()
                .organization(organization)
                .type(type)
                .name(name)
                .environment(environment)
                .build();
        return accountRepository.save(account);
    }

    public Account getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new com.argent.common.exception.NotFoundException("Account", accountId.toString()));
    }
}
