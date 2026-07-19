package com.argent.module.reporting.service;

import com.argent.module.reporting.dto.StatementLineResponse;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatementExportService {

    private final TransactionRepository transactionRepository;

    public List<StatementLineResponse> getStatementLines(UUID organizationId, String environment, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        Page<Transaction> transactions = transactionRepository.findByOrganizationIdAndEnvironmentAndDateRange(
                organizationId,
                environment,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                PageRequest.of(0, 10000));

        return transactions.getContent().stream()
                .map(this::toStatementLine)
                .collect(Collectors.toList());
    }

    public String exportCsv(UUID organizationId, String environment, LocalDate startDate, LocalDate endDate) {
        List<StatementLineResponse> lines = getStatementLines(organizationId, environment, startDate, endDate);

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Description,Amount,Status\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (StatementLineResponse line : lines) {
            csv.append(line.date().format(formatter)).append(",");
            csv.append(escapeCsv(line.type())).append(",");
            csv.append(escapeCsv(line.description() != null ? line.description() : "")).append(",");
            csv.append(line.amount()).append(",");
            csv.append(line.status()).append("\n");
        }

        return csv.toString();
    }

    private StatementLineResponse toStatementLine(Transaction tx) {
        return new StatementLineResponse(
                tx.getCreatedAt(),
                tx.getType().name(),
                tx.getDescription(),
                tx.getAmount(),
                tx.getStatus().name()
        );
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
