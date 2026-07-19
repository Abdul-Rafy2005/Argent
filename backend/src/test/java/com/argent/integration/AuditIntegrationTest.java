package com.argent.integration;

import com.argent.module.auth.dto.AuthResponse;
import com.argent.module.auth.dto.RegisterRequest;
import com.argent.module.audit.dto.AuditLogResponse;
import com.argent.module.wallet.dto.CreateWalletRequest;
import com.argent.module.wallet.dto.UpdateWalletRequest;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditIntegrationTest {

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

    @Test
    void should_query_audit_logs_after_wallet_creation() throws Exception {
        AuthResponse auth = registerAndGetAuth("audit@example.com", "Audit Org");

        CreateWalletRequest walletRequest = new CreateWalletRequest("Audit Wallet", Wallet.Type.CUSTOMER, null);
        mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("entityType", "WALLET")
                        .param("action", "CREATED"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"success\":true");
        assertThat(body).contains("\"WALLET\"");
        assertThat(body).contains("\"CREATED\"");
    }

    @Test
    void should_get_audit_log_detail_with_before_after_state() throws Exception {
        AuthResponse auth = registerAndGetAuth("auditdetail@example.com", "AuditDetail Org");

        CreateWalletRequest createRequest = new CreateWalletRequest("Original Label", Wallet.Type.CUSTOMER, null);
        MvcResult createResult = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        ApiResponse<WalletResponse> createResponse = objectMapper.readValue(createBody,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, WalletResponse.class));
        UUID walletId = createResponse.getData().id();

        UpdateWalletRequest updateRequest = new UpdateWalletRequest("Updated Label", null);
        mockMvc.perform(patch("/api/v1/wallets/" + walletId)
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        MvcResult auditResult = mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .param("entityType", "WALLET")
                        .param("action", "UPDATED"))
                .andExpect(status().isOk())
                .andReturn();

        String auditBody = auditResult.getResponse().getContentAsString();
        assertThat(auditBody).contains("\"UPDATED\"");
        assertThat(auditBody).contains("\"previousState\"");
        assertThat(auditBody).contains("\"newState\"");
    }
}
