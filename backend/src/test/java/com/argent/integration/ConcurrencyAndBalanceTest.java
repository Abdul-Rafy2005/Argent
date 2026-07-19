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
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrencyAndBalanceTest {

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
    void should_prevent_concurrent_withdrawals_from_overdrawing() throws Exception {
        AuthResponse auth = registerAndGetAuth("concurrent@example.com", "Concurrent Org");
        WalletResponse wallet = createWallet(auth, "Concurrent Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("150.00"),
                null,
                "Fund wallet",
                "concurrent-deposit",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Integer> withdrawalTask = (Callable<Integer>) () -> {
            readyLatch.countDown();
            try {
                startLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 500;
            }
            WithdrawalRequest withdrawRequest = new WithdrawalRequest(
                    wallet.id().toString(),
                    new BigDecimal("100.00"),
                    null,
                    "Concurrent withdrawal",
                    "concurrent-1",
                    null);
            try {
                MvcResult result = mockMvc.perform(post("/api/v1/transactions/withdraw")
                                .header("Authorization", "Bearer " + auth.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(withdrawRequest)))
                        .andReturn();
                return result.getResponse().getStatus();
            } catch (Exception e) {
                return 500;
            }
        };

        Callable<Integer> withdrawalTask2 = (Callable<Integer>) () -> {
            readyLatch.countDown();
            try {
                startLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 500;
            }
            WithdrawalRequest withdrawRequest = new WithdrawalRequest(
                    wallet.id().toString(),
                    new BigDecimal("100.00"),
                    null,
                    "Concurrent withdrawal",
                    "concurrent-2",
                    null);
            try {
                MvcResult result = mockMvc.perform(post("/api/v1/transactions/withdraw")
                                .header("Authorization", "Bearer " + auth.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(withdrawRequest)))
                        .andReturn();
                return result.getResponse().getStatus();
            } catch (Exception e) {
                return 500;
            }
        };

        Future<Integer> future1 = executor.submit(withdrawalTask);
        Future<Integer> future2 = executor.submit(withdrawalTask2);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        int status1 = future1.get(10, TimeUnit.SECONDS);
        int status2 = future2.get(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(status1).isIn(201, 400, 422, 409);
        assertThat(status2).isIn(201, 400, 422, 409);
        assertThat(status1).isNotEqualTo(status2);

        MvcResult walletResult = mockMvc.perform(get("/api/v1/wallets/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        WalletResponse walletResponse = extractWalletResponse(walletResult);
        assertThat(walletResponse.balance()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void should_not_mutate_balance_on_failed_withdrawal() throws Exception {
        AuthResponse auth = registerAndGetAuth("nomutate@example.com", "NoMutate Org");
        WalletResponse wallet = createWallet(auth, "NoMutate Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Initial deposit",
                "nomutate-deposit",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        MvcResult balanceCheck = mockMvc.perform(get("/api/v1/wallets/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        WalletResponse afterDeposit = extractWalletResponse(balanceCheck);
        assertThat(afterDeposit.balance()).isEqualByComparingTo(new BigDecimal("100.00"));

        WithdrawalRequest withdrawRequest = new WithdrawalRequest(
                wallet.id().toString(),
                new BigDecimal("200.00"),
                null,
                "Overdraw attempt",
                "nomutate-withdraw",
                null);

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isUnprocessableEntity());

        MvcResult balanceAfter = mockMvc.perform(get("/api/v1/wallets/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        WalletResponse afterFailedWithdrawal = extractWalletResponse(balanceAfter);
        assertThat(afterFailedWithdrawal.balance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void should_not_mutate_balance_on_failed_transfer() throws Exception {
        AuthResponse auth = registerAndGetAuth("nomutatexfer@example.com", "NoMutateXfer Org");
        WalletResponse sourceWallet = createWallet(auth, "Source Wallet");
        WalletResponse destWallet = createWallet(auth, "Dest Wallet");

        DepositRequest depositRequest = new DepositRequest(
                sourceWallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Fund source wallet",
                "nomutatexfer-deposit",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        MvcResult balanceCheck = mockMvc.perform(get("/api/v1/wallets/" + sourceWallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        WalletResponse afterDeposit = extractWalletResponse(balanceCheck);
        assertThat(afterDeposit.balance()).isEqualByComparingTo(new BigDecimal("100.00"));

        TransferRequest transferRequest = new TransferRequest(
                sourceWallet.id().toString(),
                destWallet.id().toString(),
                new BigDecimal("200.00"),
                null,
                "Overdraw transfer",
                "nomutatexfer-transfer",
                null);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isUnprocessableEntity());

        MvcResult sourceBalanceAfter = mockMvc.perform(get("/api/v1/wallets/" + sourceWallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        WalletResponse afterFailedTransfer = extractWalletResponse(sourceBalanceAfter);
        assertThat(afterFailedTransfer.balance()).isEqualByComparingTo(new BigDecimal("100.00"));

        MvcResult destBalanceAfter = mockMvc.perform(get("/api/v1/wallets/" + destWallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        WalletResponse destAfterFailedTransfer = extractWalletResponse(destBalanceAfter);
        assertThat(destAfterFailedTransfer.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_reject_operations_on_frozen_wallet() throws Exception {
        AuthResponse auth = registerAndGetAuth("frozen@example.com", "Frozen Org");
        WalletResponse wallet = createWallet(auth, "Frozen Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Fund frozen wallet",
                "frozen-deposit",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallets/" + wallet.id() + "/freeze")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk());

        DepositRequest depositAfterFreeze = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("50.00"),
                null,
                "Deposit to frozen wallet",
                "frozen-deposit-after",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositAfterFreeze)))
                .andExpect(status().isUnprocessableEntity());

        WithdrawalRequest withdrawFromFrozen = new WithdrawalRequest(
                wallet.id().toString(),
                new BigDecimal("25.00"),
                null,
                "Withdraw from frozen wallet",
                "frozen-withdraw",
                null);

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawFromFrozen)))
                .andExpect(status().isUnprocessableEntity());

        WalletResponse destWallet = createWallet(auth, "Dest Wallet");

        TransferRequest transferFromFrozen = new TransferRequest(
                wallet.id().toString(),
                destWallet.id().toString(),
                new BigDecimal("25.00"),
                null,
                "Transfer from frozen wallet",
                "frozen-transfer",
                null);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferFromFrozen)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void should_reject_operations_on_closed_wallet() throws Exception {
        AuthResponse auth = registerAndGetAuth("closed@example.com", "Closed Org");
        WalletResponse wallet = createWallet(auth, "Closed Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Fund closed wallet",
                "closed-deposit",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallets/" + wallet.id() + "/close")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk());

        DepositRequest depositAfterClose = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("50.00"),
                null,
                "Deposit to closed wallet",
                "closed-deposit-after",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositAfterClose)))
                .andExpect(status().isUnprocessableEntity());

        WithdrawalRequest withdrawFromClosed = new WithdrawalRequest(
                wallet.id().toString(),
                new BigDecimal("25.00"),
                null,
                "Withdraw from closed wallet",
                "closed-withdraw",
                null);

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawFromClosed)))
                .andExpect(status().isUnprocessableEntity());
    }
}
