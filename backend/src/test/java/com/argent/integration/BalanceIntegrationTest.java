package com.argent.integration;

import com.argent.module.auth.dto.AuthResponse;
import com.argent.module.auth.dto.RegisterRequest;
import com.argent.module.transaction.dto.DepositRequest;
import com.argent.module.transaction.dto.TransferRequest;
import com.argent.module.wallet.dto.BalanceResponse;
import com.argent.module.wallet.dto.CreateWalletRequest;
import com.argent.module.wallet.dto.WalletResponse;
import com.argent.module.wallet.entity.Wallet;
import com.argent.common.response.ApiResponse;
import com.argent.common.response.PagedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
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
class BalanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE audit_logs, api_keys, ledger_entries, balance_history, transactions, wallets, balances, accounts, users, organizations CASCADE");
        try {
            var keys = redisTemplate.keys("balance:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
        }
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
    private BalanceResponse extractBalanceResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<BalanceResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, BalanceResponse.class));
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
    void should_get_balance_after_deposit() throws Exception {
        AuthResponse auth = registerAndGetAuth("balance@example.com", "Balance Org");
        WalletResponse wallet = createWallet(auth, "Balance Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("250.00"),
                null,
                "Initial deposit",
                "balance-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/balances/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        BalanceResponse balance = extractBalanceResponse(result);
        assertThat(balance.current()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(balance.available()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(balance.pending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.reserved()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void should_get_correct_balances_after_transfer() throws Exception {
        AuthResponse auth = registerAndGetAuth("xferbalance@example.com", "XferBalance Org");
        WalletResponse sourceWallet = createWallet(auth, "Source");
        WalletResponse destWallet = createWallet(auth, "Dest");

        DepositRequest depositRequest = new DepositRequest(
                sourceWallet.id().toString(),
                new BigDecimal("500.00"),
                null,
                "Fund source",
                "xferbalance-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        TransferRequest transferRequest = new TransferRequest(
                sourceWallet.id().toString(),
                destWallet.id().toString(),
                new BigDecimal("200.00"),
                null,
                "Transfer",
                "xferbalance-transfer-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated());

        MvcResult sourceResult = mockMvc.perform(get("/api/v1/balances/" + sourceWallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        BalanceResponse sourceBalance = extractBalanceResponse(sourceResult);
        assertThat(sourceBalance.current()).isEqualByComparingTo(new BigDecimal("300.00"));

        MvcResult destResult = mockMvc.perform(get("/api/v1/balances/" + destWallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        BalanceResponse destBalance = extractBalanceResponse(destResult);
        assertThat(destBalance.current()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void should_return_fresh_value_after_cache_invalidation() throws Exception {
        AuthResponse auth = registerAndGetAuth("cachebalance@example.com", "CacheBalance Org");
        WalletResponse wallet = createWallet(auth, "Cache Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Initial",
                "cache-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        MvcResult firstResult = mockMvc.perform(get("/api/v1/balances/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        BalanceResponse firstBalance = extractBalanceResponse(firstResult);
        assertThat(firstBalance.current()).isEqualByComparingTo(new BigDecimal("100.00"));

        DepositRequest secondDeposit = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("50.00"),
                null,
                "Second deposit",
                "cache-deposit-002",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondDeposit)))
                .andExpect(status().isCreated());

        MvcResult secondResult = mockMvc.perform(get("/api/v1/balances/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        BalanceResponse secondBalance = extractBalanceResponse(secondResult);
        assertThat(secondBalance.current()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void should_return_balance_history() throws Exception {
        AuthResponse auth = registerAndGetAuth("historybalance@example.com", "HistoryBalance Org");
        WalletResponse wallet = createWallet(auth, "History Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Deposit",
                "history-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/balances/" + wallet.id() + "/history")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"success\":true");
        assertThat(body).contains("\"content\"");
    }

    @Test
    void should_return_consistent_results_under_concurrent_reads_and_writes() throws Exception {
        AuthResponse auth = registerAndGetAuth("concurrentbalance@example.com", "ConcurrentBalance Org");
        WalletResponse wallet = createWallet(auth, "Concurrent Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("1000.00"),
                null,
                "Initial fund",
                "concurrent-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(4);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);

        java.util.concurrent.Callable<BigDecimal> readTask = () -> {
            readyLatch.countDown();
            try {
                startLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return BigDecimal.ZERO;
            }
            try {
                MvcResult result = mockMvc.perform(get("/api/v1/balances/" + wallet.id())
                                .header("Authorization", "Bearer " + auth.accessToken()))
                        .andExpect(status().isOk())
                        .andReturn();
                BalanceResponse balance = extractBalanceResponse(result);
                return balance.current();
            } catch (Exception e) {
                return BigDecimal.valueOf(-1);
            }
        };

        java.util.concurrent.Callable<BigDecimal> writeTask = () -> {
            readyLatch.countDown();
            try {
                startLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return BigDecimal.ZERO;
            }
            try {
                DepositRequest creditRequest = new DepositRequest(
                        wallet.id().toString(),
                        new BigDecimal("50.00"),
                        null,
                        "Concurrent credit",
                        "concurrent-credit-" + UUID.randomUUID(),
                        null);
                mockMvc.perform(post("/api/v1/transactions/deposit")
                                .header("Authorization", "Bearer " + auth.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(creditRequest)))
                        .andExpect(status().isCreated());
                return new BigDecimal("50.00");
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        };

        java.util.List<java.util.concurrent.Future<BigDecimal>> futures = new java.util.ArrayList<>();
        futures.add(executor.submit(readTask));
        futures.add(executor.submit(readTask));
        futures.add(executor.submit(readTask));
        futures.add(executor.submit(writeTask));

        readyLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        startLatch.countDown();

        java.util.List<BigDecimal> results = new java.util.ArrayList<>();
        for (java.util.concurrent.Future<BigDecimal> f : futures) {
            results.add(f.get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        MvcResult finalResult = mockMvc.perform(get("/api/v1/balances/" + wallet.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        BalanceResponse finalBalance = extractBalanceResponse(finalResult);

        for (BigDecimal readValue : results) {
            if (readValue.compareTo(BigDecimal.ZERO) > 0) {
                assertThat(readValue.compareTo(finalBalance.current()) <= 0)
                        .as("Read value %s must not exceed final balance %s", readValue, finalBalance.current())
                        .isTrue();
            }
        }

        assertThat(finalBalance.current()).isEqualByComparingTo(new BigDecimal("1050.00"));
    }
}
