package com.argent.integration;

import com.argent.module.auth.dto.AuthResponse;
import com.argent.module.auth.dto.RegisterRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiTenancyTest {

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
    void should_isolate_organizations() throws Exception {
        AuthResponse orgA = registerAndGetAuth("orga@example.com", "Org A");
        AuthResponse orgB = registerAndGetAuth("orgb@example.com", "Org B");

        mockMvc.perform(get("/api/v1/organizations/" + orgA.user().organizationId())
                        .header("Authorization", "Bearer " + orgA.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Org A"));

        mockMvc.perform(get("/api/v1/organizations/" + orgB.user().organizationId())
                        .header("Authorization", "Bearer " + orgB.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Org B"));
    }

    @Test
    void should_reject_unauthenticated_access() throws Exception {
        mockMvc.perform(get("/api/v1/organizations"))
                .andExpect(status().isUnauthorized());
    }
}
