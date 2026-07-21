package com.argent.integration;

import com.argent.module.auth.dto.AuthResponse;
import com.argent.module.auth.dto.RegisterRequest;
import com.argent.module.transaction.dto.DepositRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE audit_logs, api_keys, ledger_entries, balance_history, transactions, wallets, balances, accounts, users, organizations CASCADE");
    }

    @SuppressWarnings("unchecked")
    private AuthResponse extractAuthResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));
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
        String body = result.getResponse().getContentAsString();
        ApiResponse<WalletResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, WalletResponse.class));
        return response.getData();
    }

    @Test
    void should_get_daily_volume_report() throws Exception {
        AuthResponse auth = registerAndGetAuth("report@example.com", "Report Org");
        WalletResponse wallet = createWallet(auth, "Report Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("250.00"),
                null,
                "Deposit",
                "report-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/reports/daily-volume")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"success\":true");
        assertThat(body).contains("\"transactionCount\"");
        assertThat(body).contains("\"totalAmount\"");
    }

    @Test
    void should_get_wallet_growth_report() throws Exception {
        AuthResponse auth = registerAndGetAuth("growth@example.com", "Growth Org");
        createWallet(auth, "Growth Wallet 1");
        createWallet(auth, "Growth Wallet 2");

        MvcResult result = mockMvc.perform(get("/api/v1/reports/wallet-growth")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"success\":true");
        assertThat(body).contains("\"walletsCreated\"");
    }

    @Test
    void should_get_transaction_report() throws Exception {
        AuthResponse auth = registerAndGetAuth("txreport@example.com", "TxReport Org");
        WalletResponse wallet = createWallet(auth, "TxReport Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "Deposit",
                "txreport-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/reports/transactions")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"success\":true");
        assertThat(body).contains("\"content\"");
    }

    @Test
    void should_export_statement_csv() throws Exception {
        AuthResponse auth = registerAndGetAuth("csv@example.com", "CSV Org");
        WalletResponse wallet = createWallet(auth, "CSV Wallet");

        DepositRequest depositRequest = new DepositRequest(
                wallet.id().toString(),
                new BigDecimal("100.00"),
                null,
                "CSV deposit",
                "csv-deposit-001",
                null);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/statements")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertThat(csv).startsWith("Date,Type,Description,Amount,Status\n");
        assertThat(csv).contains("DEPOSIT");
        assertThat(csv).contains("CSV deposit");
        assertThat(csv).contains("100.00");
    }

    @Test
    void should_return_empty_results_when_no_data() throws Exception {
        AuthResponse auth = registerAndGetAuth("emptyreport@example.com", "EmptyReport Org");

        MvcResult volumeResult = mockMvc.perform(get("/api/v1/reports/daily-volume")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(volumeResult.getResponse().getContentAsString()).contains("\"success\":true");

        MvcResult growthResult = mockMvc.perform(get("/api/v1/reports/wallet-growth")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(growthResult.getResponse().getContentAsString()).contains("\"success\":true");

        MvcResult txResult = mockMvc.perform(get("/api/v1/reports/transactions")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(txResult.getResponse().getContentAsString()).contains("\"success\":true");

        MvcResult stmtResult = mockMvc.perform(get("/api/v1/statements")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        String csv = stmtResult.getResponse().getContentAsString();
        assertThat(csv).startsWith("Date,Type,Description,Amount,Status\n");
        assertThat(csv.split("\n")).hasSize(1);
    }
    @Test
    void should_enforce_environment_isolation_for_api_keys() throws Exception {
        AuthResponse auth = registerAndGetAuth("iso@example.com", "Iso Org");

        // Create Sandbox API Key
        com.argent.module.auth.dto.CreateApiKeyRequest sandKeyReq = new com.argent.module.auth.dto.CreateApiKeyRequest("SandKey", com.argent.module.auth.entity.ApiKey.Environment.SANDBOX);
        MvcResult sandKeyRes = mockMvc.perform(post("/api/v1/api-keys")
                .header("Authorization", "Bearer " + auth.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sandKeyReq)))
                .andExpect(status().isCreated()).andReturn();
        @SuppressWarnings("unchecked")
        ApiResponse<com.argent.module.auth.dto.ApiKeyResponse> sandKeyResp = objectMapper.readValue(sandKeyRes.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, com.argent.module.auth.dto.ApiKeyResponse.class));
        com.argent.module.auth.dto.ApiKeyResponse sandKey = sandKeyResp.getData();

        // Create Production API Key
        com.argent.module.auth.dto.CreateApiKeyRequest prodKeyReq = new com.argent.module.auth.dto.CreateApiKeyRequest("ProdKey", com.argent.module.auth.entity.ApiKey.Environment.PRODUCTION);
        MvcResult prodKeyRes = mockMvc.perform(post("/api/v1/api-keys")
                .header("Authorization", "Bearer " + auth.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prodKeyReq)))
                .andExpect(status().isCreated()).andReturn();
        @SuppressWarnings("unchecked")
        ApiResponse<com.argent.module.auth.dto.ApiKeyResponse> prodKeyResp = objectMapper.readValue(prodKeyRes.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, com.argent.module.auth.dto.ApiKeyResponse.class));
        com.argent.module.auth.dto.ApiKeyResponse prodKey = prodKeyResp.getData();

        // Create wallets using API keys to set their environment automatically
        CreateWalletRequest walletRequest1 = new CreateWalletRequest("Sand Wallet", Wallet.Type.CUSTOMER, null);
        MvcResult wallet1Res = mockMvc.perform(post("/api/v1/wallets")
                .header("X-Api-Key", sandKey.rawKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(walletRequest1)))
                .andExpect(status().isCreated()).andReturn();
        @SuppressWarnings("unchecked")
        ApiResponse<WalletResponse> sandWalletResp = objectMapper.readValue(wallet1Res.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, WalletResponse.class));
        WalletResponse sandWallet = sandWalletResp.getData();

        CreateWalletRequest walletRequest2 = new CreateWalletRequest("Prod Wallet", Wallet.Type.CUSTOMER, null);
        MvcResult wallet2Res = mockMvc.perform(post("/api/v1/wallets")
                .header("X-Api-Key", prodKey.rawKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(walletRequest2)))
                .andExpect(status().isCreated()).andReturn();
        @SuppressWarnings("unchecked")
        ApiResponse<WalletResponse> prodWalletResp = objectMapper.readValue(wallet2Res.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, WalletResponse.class));
        WalletResponse prodWallet = prodWalletResp.getData();

        // Deposit into Sandbox Wallet
        DepositRequest depositSand = new DepositRequest(sandWallet.id().toString(), new BigDecimal("100.00"), null, "Sand Dep", "sand-dep", null);
        mockMvc.perform(post("/api/v1/transactions/deposit").header("X-Api-Key", sandKey.rawKey()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(depositSand))).andExpect(status().isCreated());

        // Deposit into Production Wallet
        DepositRequest depositProd = new DepositRequest(prodWallet.id().toString(), new BigDecimal("200.00"), null, "Prod Dep", "prod-dep", null);
        mockMvc.perform(post("/api/v1/transactions/deposit").header("X-Api-Key", prodKey.rawKey()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(depositProd))).andExpect(status().isCreated());

        // Verify Sandbox key sees only 100
        MvcResult reportSand = mockMvc.perform(get("/api/v1/reports/daily-volume").header("X-Api-Key", sandKey.rawKey())).andReturn();
        assertThat(reportSand.getResponse().getContentAsString()).contains("\"totalAmount\":100.00");
        assertThat(reportSand.getResponse().getContentAsString()).doesNotContain("\"totalAmount\":200.00");

        // Verify Production key sees only 200
        MvcResult reportProd = mockMvc.perform(get("/api/v1/reports/daily-volume").header("X-Api-Key", prodKey.rawKey())).andReturn();
        assertThat(reportProd.getResponse().getContentAsString()).contains("\"totalAmount\":200.00");
        assertThat(reportProd.getResponse().getContentAsString()).doesNotContain("\"totalAmount\":100.00");

        // Verify JWT sees both (300 total)
        MvcResult reportAll = mockMvc.perform(get("/api/v1/reports/daily-volume").header("Authorization", "Bearer " + auth.accessToken())).andReturn();
        assertThat(reportAll.getResponse().getContentAsString()).contains("\"totalAmount\":300.00");
    }
}
