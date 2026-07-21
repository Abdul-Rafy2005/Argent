package com.argent.module.reporting;

import com.argent.module.reporting.dto.DailyVolumeResponse;
import com.argent.module.reporting.dto.TransactionReportResponse;
import com.argent.module.reporting.dto.WalletGrowthResponse;
import com.argent.module.reporting.service.ReportingService;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private ReportingService reportingService;

    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
    }

    @Test
    void should_aggregate_daily_volume() {
        Transaction tx1 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build();
        Transaction tx2 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(Transaction.Type.WITHDRAWAL)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("50.00"))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Transaction> page = new PageImpl<>(List.of(tx1, tx2));
        when(transactionRepository.findByOrganizationIdAndEnvironmentAndDateRange(
                eq(orgId), isNull(), any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);

        List<DailyVolumeResponse> results = reportingService.getDailyVolume(orgId, null, LocalDate.now(), LocalDate.now());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).transactionCount()).isEqualTo(2);
        assertThat(results.get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void should_return_empty_results_for_no_transactions() {
        Page<Transaction> emptyPage = new PageImpl<>(List.of());
        when(transactionRepository.findByOrganizationIdAndEnvironmentAndDateRange(
                eq(orgId), isNull(), any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        List<DailyVolumeResponse> results = reportingService.getDailyVolume(orgId, null, LocalDate.now(), LocalDate.now());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).transactionCount()).isEqualTo(0);
        assertThat(results.get(0).totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_return_wallet_growth() {
        when(walletRepository.countByOrganizationIdAndEnvironmentAndCreatedAtBetween(
                eq(orgId), isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L);

        List<WalletGrowthResponse> results = reportingService.getWalletGrowth(orgId, null, LocalDate.now(), LocalDate.now());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).walletsCreated()).isEqualTo(3);
    }

    @Test
    void should_filter_transactions_by_type() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build();

        Page<Transaction> page = new PageImpl<>(List.of(tx));
        when(transactionRepository.findByFilters(
                eq(orgId), isNull(), eq(Transaction.Type.DEPOSIT), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<TransactionReportResponse> results = reportingService.getTransactions(
                orgId, null, Transaction.Type.DEPOSIT, null, null, null, 0, 20);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).type()).isEqualTo("DEPOSIT");
    }
}
