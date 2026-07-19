package com.argent.module.reporting;

import com.argent.module.reporting.service.StatementExportService;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.organization.entity.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementExportTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private StatementExportService statementExportService;

    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
    }

    @Test
    void should_generate_csv_with_header_and_rows() {
        Transaction tx1 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("100.00"))
                .description("Initial deposit")
                .createdAt(LocalDateTime.of(2026, 7, 19, 10, 30, 0))
                .build();
        Transaction tx2 = Transaction.builder()
                .id(UUID.randomUUID())
                .type(Transaction.Type.WITHDRAWAL)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("50.00"))
                .description("Cash out")
                .createdAt(LocalDateTime.of(2026, 7, 19, 14, 0, 0))
                .build();

        Page<Transaction> page = new PageImpl<>(List.of(tx1, tx2));
        when(transactionRepository.findByOrganizationIdAndDateRange(
                eq(orgId), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(page);

        String csv = statementExportService.exportCsv(orgId, LocalDate.of(2026, 7, 19), LocalDate.of(2026, 7, 19));

        assertThat(csv).startsWith("Date,Type,Description,Amount,Status\n");
        assertThat(csv).contains("DEPOSIT");
        assertThat(csv).contains("Initial deposit");
        assertThat(csv).contains("100.00");
        assertThat(csv).contains("WITHDRAWAL");
        assertThat(csv).contains("Cash out");
        assertThat(csv).contains("50.00");
    }

    @Test
    void should_return_only_header_for_empty_data() {
        Page<Transaction> emptyPage = new PageImpl<>(List.of());
        when(transactionRepository.findByOrganizationIdAndDateRange(
                eq(orgId), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(emptyPage);

        String csv = statementExportService.exportCsv(orgId, LocalDate.now(), LocalDate.now());

        assertThat(csv).startsWith("Date,Type,Description,Amount,Status\n");
        assertThat(csv.split("\n")).hasSize(1);
    }

    @Test
    void should_escape_csv_values_with_commas() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .type(Transaction.Type.DEPOSIT)
                .status(Transaction.Status.COMPLETED)
                .amount(new BigDecimal("100.00"))
                .description("Deposit, with comma")
                .createdAt(LocalDateTime.now())
                .build();

        Page<Transaction> page = new PageImpl<>(List.of(tx));
        when(transactionRepository.findByOrganizationIdAndDateRange(
                eq(orgId), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(page);

        String csv = statementExportService.exportCsv(orgId, LocalDate.now(), LocalDate.now());

        assertThat(csv).contains("\"Deposit, with comma\"");
    }
}
