package com.argent.module.reporting.service;

import com.argent.module.reporting.dto.DailyVolumeResponse;
import com.argent.module.reporting.dto.TransactionReportResponse;
import com.argent.module.reporting.dto.WalletGrowthResponse;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public List<DailyVolumeResponse> getDailyVolume(UUID organizationId, String environment, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        List<DailyVolumeResponse> results = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            LocalDateTime dayStart = current.atStartOfDay();
            LocalDateTime dayEnd = current.atTime(LocalTime.MAX);

            Page<Transaction> transactions = transactionRepository.findByOrganizationIdAndEnvironmentAndDateRange(
                    organizationId, environment, dayStart, dayEnd, PageRequest.of(0, 10000));

            long count = transactions.getTotalElements();
            BigDecimal totalAmount = transactions.getContent().stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            results.add(new DailyVolumeResponse(current, count, totalAmount));
            current = current.plusDays(1);
        }
        return results;
    }

    public List<WalletGrowthResponse> getWalletGrowth(UUID organizationId, String environment, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        List<WalletGrowthResponse> results = new ArrayList<>();
        LocalDate current = startDate;
        Wallet.Environment envEnum = environment != null ? Wallet.Environment.valueOf(environment) : null;
        while (!current.isAfter(endDate)) {
            long count = walletRepository.countByOrganizationIdAndEnvironmentAndCreatedAtBetween(
                    organizationId, envEnum, current.atStartOfDay(), current.atTime(LocalTime.MAX));
            results.add(new WalletGrowthResponse(current, count));
            current = current.plusDays(1);
        }
        return results;
    }

    public Page<TransactionReportResponse> getTransactions(
            UUID organizationId,
            String environment,
            Transaction.Type type,
            Transaction.Status status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize);

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        return transactionRepository.findByFilters(
                organizationId, environment, type, status, start, end, pageable
        ).map(this::toTransactionReport);
    }

    private TransactionReportResponse toTransactionReport(Transaction tx) {
        return new TransactionReportResponse(
                tx.getId(),
                tx.getType().name(),
                tx.getStatus().name(),
                tx.getAmount(),
                tx.getEnvironment(),
                tx.getCreatedAt()
        );
    }
}
