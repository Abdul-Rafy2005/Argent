package com.argent.integration;

import com.argent.module.auth.dto.AuthResponse;
import com.argent.module.auth.dto.RegisterRequest;
import com.argent.common.response.ApiResponse;
import com.argent.module.ledger.dto.LedgerEntryResponse;
import com.argent.module.ledger.dto.ReconciliationResponse;
import com.argent.module.wallet.dto.CreateWalletRequest;
import com.argent.module.wallet.dto.WalletResponse;
import com.argent.module.wallet.entity.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.dao.DataAccessException;
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
class LedgerIntegrationTest {

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
    private LedgerEntryResponse extractLedgerEntryResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<LedgerEntryResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LedgerEntryResponse.class));
        return response.getData();
    }

    @SuppressWarnings("unchecked")
    private ReconciliationResponse extractReconciliationResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<ReconciliationResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ReconciliationResponse.class));
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
    void should_query_ledger_entries_for_account() throws Exception {
        AuthResponse auth = registerAndGetAuth("ledger1@example.com", "Ledger Org");
        WalletResponse wallet = createWallet(auth, "Test Wallet");

        mockMvc.perform(get("/api/v1/ledger/entries")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("accountId", wallet.accountId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_get_single_ledger_entry() throws Exception {
        AuthResponse auth = registerAndGetAuth("ledger2@example.com", "Ledger Org 2");
        WalletResponse wallet = createWallet(auth, "Test Wallet 2");

        mockMvc.perform(get("/api/v1/ledger/entries/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_reconcile_account() throws Exception {
        AuthResponse auth = registerAndGetAuth("reconcile@example.com", "Reconcile Org");
        WalletResponse wallet = createWallet(auth, "Reconcile Wallet");

        MvcResult result = mockMvc.perform(get("/api/v1/ledger/reconcile")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("accountId", wallet.accountId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        ReconciliationResponse reconResponse = extractReconciliationResponse(result);
        assertThat(reconResponse.reconciled()).isTrue();
        assertThat(reconResponse.ledgerBalance()).isEqualByComparingTo(reconResponse.storedBalance());
    }

    @Test
    void should_reject_unauthenticated_ledger_access() throws Exception {
        mockMvc.perform(get("/api/v1/ledger/entries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_cross_org_ledger_access() throws Exception {
        AuthResponse auth1 = registerAndGetAuth("org-a@example.com", "Org A");
        AuthResponse auth2 = registerAndGetAuth("org-b@example.com", "Org B");

        WalletResponse wallet = createWallet(auth1, "Org A Wallet");

        mockMvc.perform(get("/api/v1/ledger/entries")
                        .header("Authorization", "Bearer " + auth2.accessToken())
                        .param("accountId", wallet.accountId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void should_list_ledger_entries_with_pagination() throws Exception {
        AuthResponse auth = registerAndGetAuth("paginate@example.com", "Paginate Org");
        WalletResponse wallet = createWallet(auth, "Paginate Wallet");

        mockMvc.perform(get("/api/v1/ledger/entries")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void should_reject_database_update_on_ledger_entries() throws Exception {
        AuthResponse auth = registerAndGetAuth("immutable-update@example.com", "Immutable Org");
        WalletResponse wallet = createWallet(auth, "Immutable Wallet");

        UUID orgId = wallet.organizationId();
        UUID accountId = wallet.accountId();

        UUID txId = jdbcTemplate.queryForObject(
                "INSERT INTO transactions (organization_id, type, status, amount, created_at) VALUES (?, 'DEPOSIT', 'COMPLETED', 100.0000, CURRENT_TIMESTAMP) RETURNING id",
                UUID.class, orgId);

        UUID entryId = jdbcTemplate.queryForObject(
                "INSERT INTO ledger_entries (organization_id, transaction_id, account_id, type, amount, balance_after, created_at) VALUES (?, ?, ?, 'DEBIT', 100.0000, 100.0000, CURRENT_TIMESTAMP) RETURNING id",
                UUID.class, orgId, txId, accountId);

        try {
            jdbcTemplate.update("UPDATE ledger_entries SET amount = 999.0000 WHERE id = ?", entryId);
            org.assertj.core.api.Assertions.fail("Expected DataAccessException from database trigger");
        } catch (DataAccessException ex) {
            assertThat(ex.getMessage()).contains("Ledger entries are immutable");
        }
    }

    @Test
    void should_reject_database_delete_on_ledger_entries() throws Exception {
        AuthResponse auth = registerAndGetAuth("immutable-delete@example.com", "Immutable Org 2");
        WalletResponse wallet = createWallet(auth, "Immutable Wallet 2");

        UUID orgId = wallet.organizationId();
        UUID accountId = wallet.accountId();

        UUID txId = jdbcTemplate.queryForObject(
                "INSERT INTO transactions (organization_id, type, status, amount, created_at) VALUES (?, 'DEPOSIT', 'COMPLETED', 50.0000, CURRENT_TIMESTAMP) RETURNING id",
                UUID.class, orgId);

        UUID entryId = jdbcTemplate.queryForObject(
                "INSERT INTO ledger_entries (organization_id, transaction_id, account_id, type, amount, balance_after, created_at) VALUES (?, ?, ?, 'CREDIT', 50.0000, 50.0000, CURRENT_TIMESTAMP) RETURNING id",
                UUID.class, orgId, txId, accountId);

        try {
            jdbcTemplate.update("DELETE FROM ledger_entries WHERE id = ?", entryId);
            org.assertj.core.api.Assertions.fail("Expected DataAccessException from database trigger");
        } catch (DataAccessException ex) {
            assertThat(ex.getMessage()).contains("Ledger entries are immutable");
        }
    }

    @Test
    void should_store_environment_on_ledger_entries() throws Exception {
        AuthResponse auth = registerAndGetAuth("env-store@example.com", "Env Store Org");
        WalletResponse wallet = createWallet(auth, "Env Wallet");

        UUID orgId = wallet.organizationId();
        UUID accountId = wallet.accountId();

        UUID txId = jdbcTemplate.queryForObject(
                "INSERT INTO transactions (organization_id, type, status, amount, created_at) VALUES (?, 'DEPOSIT', 'COMPLETED', 100.0000, CURRENT_TIMESTAMP) RETURNING id",
                UUID.class, orgId);

        UUID entryId = jdbcTemplate.queryForObject(
                "INSERT INTO ledger_entries (organization_id, transaction_id, account_id, type, amount, balance_after, environment, created_at) VALUES (?, ?, ?, 'DEBIT', 100.0000, 100.0000, 'SANDBOX', CURRENT_TIMESTAMP) RETURNING id",
                UUID.class, orgId, txId, accountId);

        String env = jdbcTemplate.queryForObject(
                "SELECT environment FROM ledger_entries WHERE id = ?", String.class, entryId);

        assertThat(env).isEqualTo("SANDBOX");
    }

    @Test
    void should_filter_ledger_entries_by_environment() throws Exception {
        AuthResponse auth = registerAndGetAuth("env-filter@example.com", "Env Filter Org");
        WalletResponse wallet = createWallet(auth, "Env Filter Wallet");

        UUID orgId = wallet.organizationId();
        UUID accountId = wallet.accountId();

        UUID txId = jdbcTemplate.queryForObject(
                "INSERT INTO transactions (organization_id, type, status, amount, created_at) VALUES (?, 'DEPOSIT', 'COMPLETED', 100.0000, CURRENT_TIMESTAMP) RETURNING id",
                UUID.class, orgId);

        jdbcTemplate.update(
                "INSERT INTO ledger_entries (organization_id, transaction_id, account_id, type, amount, balance_after, environment, created_at) VALUES (?, ?, ?, 'DEBIT', 100.0000, 100.0000, 'SANDBOX', CURRENT_TIMESTAMP)",
                orgId, txId, accountId);

        jdbcTemplate.update(
                "INSERT INTO ledger_entries (organization_id, transaction_id, account_id, type, amount, balance_after, environment, created_at) VALUES (?, ?, ?, 'CREDIT', 100.0000, 100.0000, 'PRODUCTION', CURRENT_TIMESTAMP)",
                orgId, txId, accountId);

        int sandboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE account_id = ? AND environment = 'SANDBOX'",
                Integer.class, accountId);

        int productionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE account_id = ? AND environment = 'PRODUCTION'",
                Integer.class, accountId);

        assertThat(sandboxCount).isEqualTo(1);
        assertThat(productionCount).isEqualTo(1);
    }
}
