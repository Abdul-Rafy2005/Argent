package com.argent.integration;

import com.argent.module.auth.dto.AuthResponse;
import com.argent.module.auth.dto.RegisterRequest;
import com.argent.module.transaction.dto.*;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.wallet.dto.CreateWalletRequest;
import com.argent.module.wallet.dto.WalletResponse;
import com.argent.module.wallet.entity.Wallet;
import com.argent.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE audit_logs, api_keys, ledger_entries, transactions, wallets, balances, accounts, users, organizations CASCADE");
    }

    @SuppressWarnings("unchecked")
    private AuthResponse extractAuthResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));
        return response.getData();
    }

    @SuppressWarnings("unchecked")
    private WalletResponse extractWalletResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<WalletResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, WalletResponse.class));
        return response.getData();
    }

    @SuppressWarnings("unchecked")
    private TransactionResponse extractTransactionResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<TransactionResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, TransactionResponse.class));
        return response.getData();
    }

    private AuthResponse registerAndGetAuth(String email, String orgName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "password123", "User", orgName);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractAuthResponse(result);
    }

    private WalletResponse createWallet(AuthResponse auth, String label) throws Exception {
        CreateWalletRequest walletRequest = new CreateWalletRequest(label, Wallet.Type.CUSTOMER, null);
        MvcResult result = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractWalletResponse(result);
    }

    @Test
    void should_deposit_funds_successfully() throws Exception {
        AuthResponse auth = registerAndGetAuth("deposit@example.com", "Deposit Org");
        WalletResponse wallet = createWallet(auth, "Deposit Wallet");

        DepositRequest request = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Initial deposit",
                "deposit-idem-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.amount").value(100.0));
    }

    @Test
    void should_withdraw_funds_successfully() throws Exception {
        AuthResponse auth = registerAndGetAuth("withdraw@example.com", "Withdraw Org");
        WalletResponse wallet = createWallet(auth, "Withdraw Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("200.00"),
                null,
                "Deposit for withdrawal",
                "withdraw-deposit-idem-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        WithdrawalRequest withdrawRequest = new WithdrawalRequest(
                wallet.id().toString(),
                new BigDecimal("50.00"),
                null,
                "Withdraw some funds",
                "withdraw-idem-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void should_transfer_funds_between_wallets() throws Exception {
        AuthResponse auth = registerAndGetAuth("transfer@example.com", "Transfer Org");
        WalletResponse sourceWallet = createWallet(auth, "Source Wallet");
        WalletResponse destWallet = createWallet(auth, "Dest Wallet");

        DepositRequest depositRequest = new DepositRequest(
                sourceWallet.id().toString(),
                new BigDecimal("500.00"),
                null,
                "Fund source",
                "transfer-deposit-idem-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        TransferRequest transferRequest = new TransferRequest(
                sourceWallet.id().toString(),
                destWallet.id().toString(),
                new BigDecimal("150.00"),
                null,
                "Transfer to dest",
                "transfer-idem-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("TRANSFER"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void should_reject_duplicate_idempotency_key() throws Exception {
        AuthResponse auth = registerAndGetAuth("idempotent@example.com", "Idempotent Org");
        WalletResponse wallet = createWallet(auth, "Idempotent Wallet");

        DepositRequest request = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "First deposit",
                "same-idem-key",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        DepositRequest duplicateRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("200.00"),
                null,
                "Duplicate deposit",
                "same-idem-key",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void should_get_transaction_by_id() throws Exception {
        AuthResponse auth = registerAndGetAuth("gettx@example.com", "GetTx Org");
        WalletResponse wallet = createWallet(auth, "GetTx Wallet");

        DepositRequest request = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("75.00"),
                null,
                "Get tx deposit",
                "gettx-idem-001",
                null);

        MvcResult result = mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse txResponse = extractTransactionResponse(result);

        mockMvc.perform(get("/api/v1/transactions/" + txResponse.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(txResponse.id().toString()))
                .andExpect(jsonPath("$.data.type").value("DEPOSIT"));
    }

    @Test
    void should_list_transactions() throws Exception {
        AuthResponse auth = registerAndGetAuth("list@example.com", "List Org");
        WalletResponse wallet = createWallet(auth, "List Wallet");

        for (int i = 1; i <= 3; i++) {
            DepositRequest request = new DepositRequest(
                    wallet.id().toString(),
                    new BigDecimal("10.00"),
                    null,
                    "Deposit " + i,
                    "list-idem-00" + i,
                    null);

            mockMvc.perform(post("/api/v1/transactions/deposit")
                            .header("Authorization", "Bearer " + auth.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3));
    }

    @Test
    void should_reject_deposit_to_nonexistent_wallet() throws Exception {
        AuthResponse auth = registerAndGetAuth("badwallet@example.com", "BadWallet Org");

        DepositRequest request = new DepositRequest(
                UUID.randomUUID().toString(),
                new BigDecimal("100.00"),
                null,
                "Bad wallet deposit",
                "badwallet-idem-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_reject_transfer_to_same_wallet() throws Exception {
        AuthResponse auth = registerAndGetAuth("selftransfer@example.com", "SelfTransfer Org");
        WalletResponse wallet = createWallet(auth, "Self Transfer Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Fund wallet",
                "selftransfer-deposit-idem-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        TransferRequest transferRequest = new TransferRequest(
                wallet.id().toString(),
                wallet.id().toString(),
                new BigDecimal("50.00"),
                null,
                "Self transfer",
                "selftransfer-idem-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());
    }

    @SuppressWarnings("unchecked")
    @Test
    void should_create_correct_ledger_entries_for_deposit() throws Exception {
        AuthResponse auth = registerAndGetAuth("ledger-deposit@example.com", "LedgerDeposit Org");
        WalletResponse wallet = createWallet(auth, "LedgerDeposit Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("150.00"),
                null,
                "Ledger test deposit",
                "ledger-deposit-001",
                null);

        MvcResult depositResult = mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse txResponse = extractTransactionResponse(depositResult);
        UUID transactionId = txResponse.id();

        MvcResult ledgerResult = mockMvc.perform(get("/api/v1/ledger/entries")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("transactionId", transactionId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String ledgerBody = ledgerResult.getResponse().getContentAsString();
        ApiResponse<?> ledgerApiResponse = objectMapper.readValue(ledgerBody,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, Object.class));
        Map<String, Object> ledgerData = (Map<String, Object>) ledgerApiResponse.getData();
        java.util.List<Map<String, Object>> ledgerContent = (java.util.List<Map<String, Object>>) ledgerData.get("content");

        assertThat(ledgerContent).hasSize(2);

        Map<String, Object> debitEntry = ledgerContent.stream()
                .filter(e -> "DEBIT".equals(e.get("type")))
                .findFirst().orElseThrow();
        Map<String, Object> creditEntry = ledgerContent.stream()
                .filter(e -> "CREDIT".equals(e.get("type")))
                .findFirst().orElseThrow();

        String debitAccountId = (String) debitEntry.get("accountId");
        String creditAccountId = (String) creditEntry.get("accountId");
        assertThat(debitAccountId).isNotEqualTo(creditAccountId);

        BigDecimal debitAmount = new BigDecimal(debitEntry.get("amount").toString());
        BigDecimal creditAmount = new BigDecimal(creditEntry.get("amount").toString());
        assertThat(debitAmount).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(creditAmount).isEqualByComparingTo(new BigDecimal("150.00"));

        MvcResult balanceResult = mockMvc.perform(get("/api/v1/balances/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        String balanceBody = balanceResult.getResponse().getContentAsString();
        assertThat(balanceBody).contains("\"current\":150.0000");

        BigDecimal creditBalanceAfter = new BigDecimal(creditEntry.get("balanceAfter").toString());
        assertThat(creditBalanceAfter).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void should_create_correct_ledger_entries_for_withdrawal() throws Exception {
        AuthResponse auth = registerAndGetAuth("ledger-withdrawal@example.com", "LedgerWithdrawal Org");
        WalletResponse wallet = createWallet(auth, "LedgerWithdrawal Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("200.00"),
                null,
                "Initial deposit",
                "ledger-withdrawal-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(
                wallet.id().toString(),
                new BigDecimal("75.00"),
                null,
                "Ledger test withdrawal",
                "ledger-withdrawal-001",
                null);

        MvcResult withdrawalResult = mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse txResponse = extractTransactionResponse(withdrawalResult);
        UUID transactionId = txResponse.id();

        MvcResult ledgerResult = mockMvc.perform(get("/api/v1/ledger/entries")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("transactionId", transactionId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String ledgerBody = ledgerResult.getResponse().getContentAsString();
        ApiResponse<?> ledgerApiResponse = objectMapper.readValue(ledgerBody,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, Object.class));
        Map<String, Object> ledgerData = (Map<String, Object>) ledgerApiResponse.getData();
        java.util.List<Map<String, Object>> ledgerContent = (java.util.List<Map<String, Object>>) ledgerData.get("content");

        assertThat(ledgerContent).hasSize(2);

        Map<String, Object> debitEntry = ledgerContent.stream()
                .filter(e -> "DEBIT".equals(e.get("type")))
                .findFirst().orElseThrow();
        Map<String, Object> creditEntry = ledgerContent.stream()
                .filter(e -> "CREDIT".equals(e.get("type")))
                .findFirst().orElseThrow();

        String debitAccountId = (String) debitEntry.get("accountId");
        String creditAccountId = (String) creditEntry.get("accountId");
        assertThat(debitAccountId).isNotEqualTo(creditAccountId);

        BigDecimal debitAmount = new BigDecimal(debitEntry.get("amount").toString());
        BigDecimal creditAmount = new BigDecimal(creditEntry.get("amount").toString());
        assertThat(debitAmount).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(creditAmount).isEqualByComparingTo(new BigDecimal("75.00"));

        MvcResult balanceResult = mockMvc.perform(get("/api/v1/balances/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        String balanceBody = balanceResult.getResponse().getContentAsString();
        assertThat(balanceBody).contains("\"current\":125.0000");

        BigDecimal debitBalanceAfter = new BigDecimal(debitEntry.get("balanceAfter").toString());
        assertThat(debitBalanceAfter).isEqualByComparingTo(new BigDecimal("125.00"));
    }
}
