package com.argent.module.wallet;

import com.argent.common.exception.NotFoundException;
import com.argent.module.ledger.repository.LedgerEntryRepository;
import com.argent.module.wallet.entity.Balance;
import com.argent.module.wallet.entity.BalanceHistory;
import com.argent.module.wallet.repository.BalanceHistoryRepository;
import com.argent.module.wallet.repository.BalanceRepository;
import com.argent.module.wallet.repository.WalletRepository;
import com.argent.module.wallet.service.BalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;
    @Mock
    private BalanceHistoryRepository balanceHistoryRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private BalanceService balanceService;

    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void should_initialize_balance_with_zeros() {
        Balance expectedBalance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .current(BigDecimal.ZERO)
                .available(BigDecimal.ZERO)
                .pending(BigDecimal.ZERO)
                .reserved(BigDecimal.ZERO)
                .build();

        when(balanceRepository.save(any(Balance.class))).thenReturn(expectedBalance);

        Balance balance = balanceService.initializeBalance(accountId);

        assertThat(balance).isNotNull();
        assertThat(balance.getCurrent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getAvailable()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getReserved()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getAccountId()).isEqualTo(accountId);
        verify(balanceRepository).save(any(Balance.class));
    }

    @Test
    void should_get_balance_by_account_id() {
        Balance balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .current(new BigDecimal("100.00"))
                .available(new BigDecimal("80.00"))
                .pending(new BigDecimal("10.00"))
                .reserved(new BigDecimal("10.00"))
                .build();

        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.of(balance));

        Balance result = balanceService.getBalance(accountId);

        assertThat(result).isNotNull();
        assertThat(result.getCurrent()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getAvailable()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void should_return_cached_balance_on_cache_hit() {
        Balance balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .current(new BigDecimal("500.00"))
                .available(new BigDecimal("500.00"))
                .pending(BigDecimal.ZERO)
                .reserved(BigDecimal.ZERO)
                .build();

        when(valueOperations.get("balance:" + accountId)).thenReturn(balance);

        Balance result = balanceService.getBalance(accountId);

        assertThat(result).isNotNull();
        assertThat(result.getCurrent()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(balanceRepository, never()).findByAccountId(any());
    }

    @Test
    void should_cache_balance_after_db_fetch() {
        Balance balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .current(new BigDecimal("200.00"))
                .available(new BigDecimal("200.00"))
                .pending(BigDecimal.ZERO)
                .reserved(BigDecimal.ZERO)
                .build();

        when(valueOperations.get("balance:" + accountId)).thenReturn(null);
        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.of(balance));

        Balance result = balanceService.getBalance(accountId);

        assertThat(result).isNotNull();
        verify(valueOperations).set(eq("balance:" + accountId), any(Balance.class), any());
    }

    @Test
    void should_invalidate_cache_on_credit() {
        Balance balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .current(new BigDecimal("100.00"))
                .available(new BigDecimal("100.00"))
                .pending(BigDecimal.ZERO)
                .reserved(BigDecimal.ZERO)
                .build();

        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.of(balance));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(balanceHistoryRepository.save(any(BalanceHistory.class))).thenReturn(null);

        balanceService.credit(accountId, new BigDecimal("50.00"));

        verify(redisTemplate).delete("balance:" + accountId);
    }

    @Test
    void should_invalidate_cache_on_debit() {
        Balance balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .current(new BigDecimal("100.00"))
                .available(new BigDecimal("100.00"))
                .pending(BigDecimal.ZERO)
                .reserved(BigDecimal.ZERO)
                .build();

        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.of(balance));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(balanceHistoryRepository.save(any(BalanceHistory.class))).thenReturn(null);

        balanceService.debit(accountId, new BigDecimal("30.00"));

        verify(redisTemplate).delete("balance:" + accountId);
    }

    @Test
    void should_throw_when_balance_not_found() {
        when(balanceRepository.findByAccountId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceService.getBalance(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_recalculate_balance_from_ledger() {
        Balance balance = Balance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .current(new BigDecimal("100.00"))
                .build();

        when(balanceRepository.findByAccountId(accountId)).thenReturn(Optional.of(balance));
        when(ledgerEntryRepository.netBalanceByAccountId(accountId)).thenReturn(new BigDecimal("350.00"));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(inv -> inv.getArgument(0));

        Balance result = balanceService.recalculateFromLedger(accountId);

        assertThat(result.getCurrent()).isEqualByComparingTo(new BigDecimal("350.00"));
        verify(balanceRepository).save(any(Balance.class));
    }

    @Test
    void should_throw_when_recalculating_for_nonexistent_balance() {
        when(balanceRepository.findByAccountId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceService.recalculateFromLedger(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
