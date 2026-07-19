package com.argent.integration;

import com.argent.module.auth.dto.AuthResponse;
import com.argent.module.auth.dto.CreateApiKeyRequest;
import com.argent.module.auth.dto.ApiKeyResponse;
import com.argent.module.auth.dto.RegisterRequest;
import com.argent.module.wallet.dto.CreateWalletRequest;
import com.argent.module.wallet.dto.UpdateWalletRequest;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletIntegrationTest {

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
    void should_create_wallet_and_verify_account_and_balance_exist() throws Exception {
        AuthResponse auth = registerAndGetAuth("wallet@example.com", "Wallet Org");

        CreateWalletRequest walletRequest = new CreateWalletRequest("My Wallet", Wallet.Type.CUSTOMER, Map.of("customerId", "cust-123"));
        MvcResult result = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.label").value("My Wallet"))
                .andExpect(jsonPath("$.data.type").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.metadata.customerId").value("cust-123"))
                .andReturn();

        WalletResponse walletResponse = extractWalletResponse(result);

        mockMvc.perform(get("/api/v1/wallets/" + walletResponse.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(walletResponse.id().toString()))
                .andExpect(jsonPath("$.data.label").value("My Wallet"));
    }

    @Test
    void should_list_wallets_only_for_authenticated_organization() throws Exception {
        AuthResponse auth1 = registerAndGetAuth("org1@example.com", "Org 1");
        AuthResponse auth2 = registerAndGetAuth("org2@example.com", "Org 2");

        CreateWalletRequest request1 = new CreateWalletRequest("Wallet 1", Wallet.Type.CUSTOMER, null);
        CreateWalletRequest request2 = new CreateWalletRequest("Wallet 2", Wallet.Type.MERCHANT, null);
        CreateWalletRequest request3 = new CreateWalletRequest("Wallet 3", Wallet.Type.CUSTOMER, null);

        mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth1.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth1.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth2.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth1.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2));

        mockMvc.perform(get("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth2.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void should_freeze_wallet_and_reject_transactions() throws Exception {
        AuthResponse auth = registerAndGetAuth("freeze@example.com", "Freeze Org");

        CreateWalletRequest walletRequest = new CreateWalletRequest("Freeze Wallet", Wallet.Type.CUSTOMER, null);
        MvcResult result = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse walletResponse = extractWalletResponse(result);

        mockMvc.perform(post("/api/v1/wallets/" + walletResponse.id() + "/freeze")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FROZEN"));
    }

    @Test
    void should_close_wallet_and_prevent_unfreeze() throws Exception {
        AuthResponse auth = registerAndGetAuth("close@example.com", "Close Org");

        CreateWalletRequest walletRequest = new CreateWalletRequest("Close Wallet", Wallet.Type.CUSTOMER, null);
        MvcResult result = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse walletResponse = extractWalletResponse(result);

        mockMvc.perform(post("/api/v1/wallets/" + walletResponse.id() + "/close")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(post("/api/v1/wallets/" + walletResponse.id() + "/unfreeze")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WALLET_CLOSED"));
    }

    @Test
    void should_update_wallet_metadata() throws Exception {
        AuthResponse auth = registerAndGetAuth("metadata@example.com", "Metadata Org");

        CreateWalletRequest createRequest = new CreateWalletRequest("Metadata Wallet", Wallet.Type.CUSTOMER, Map.of("initial", "value"));
        MvcResult result = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse walletResponse = extractWalletResponse(result);

        UpdateWalletRequest updateRequest = new UpdateWalletRequest("Updated Label", Map.of("key", "new-value", "another", "123"));
        mockMvc.perform(patch("/api/v1/wallets/" + walletResponse.id())
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.label").value("Updated Label"))
                .andExpect(jsonPath("$.data.metadata.key").value("new-value"))
                .andExpect(jsonPath("$.data.metadata.another").value("123"));

        mockMvc.perform(get("/api/v1/wallets/" + walletResponse.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("Updated Label"))
                .andExpect(jsonPath("$.data.metadata.key").value("new-value"));
    }

    @Test
    void should_reject_access_to_other_organizations_wallet() throws Exception {
        AuthResponse auth1 = registerAndGetAuth("owner1@example.com", "Owner 1 Org");
        AuthResponse auth2 = registerAndGetAuth("owner2@example.com", "Owner 2 Org");

        CreateWalletRequest walletRequest = new CreateWalletRequest("Private Wallet", Wallet.Type.CUSTOMER, null);
        MvcResult result = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth1.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse walletResponse = extractWalletResponse(result);

        mockMvc.perform(get("/api/v1/wallets/" + walletResponse.id())
                        .header("Authorization", "Bearer " + auth2.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_create_sandbox_wallet_by_default_for_jwt_auth() throws Exception {
        AuthResponse auth = registerAndGetAuth("sandbox@example.com", "Sandbox Org");

        CreateWalletRequest walletRequest = new CreateWalletRequest("Default Wallet", Wallet.Type.CUSTOMER, null);
        mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.environment").value("SANDBOX"));
    }

    @SuppressWarnings("unchecked")
    private ApiKeyResponse extractApiKeyResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<ApiKeyResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ApiKeyResponse.class));
        return response.getData();
    }

    private ApiKeyResponse createApiKey(AuthResponse auth, String name, String environment) throws Exception {
        CreateApiKeyRequest request = new CreateApiKeyRequest(name,
                com.argent.module.auth.entity.ApiKey.Environment.valueOf(environment));
        MvcResult result = mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractApiKeyResponse(result);
    }

    @Test
    void should_reject_sandbox_api_key_accessing_production_wallet() throws Exception {
        AuthResponse auth = registerAndGetAuth("env-cross@example.com", "Env Cross Org");

        ApiKeyResponse sandboxKey = createApiKey(auth, "Sandbox Key", "SANDBOX");
        ApiKeyResponse productionKey = createApiKey(auth, "Production Key", "PRODUCTION");

        CreateWalletRequest prodWalletRequest = new CreateWalletRequest("Prod Wallet", Wallet.Type.CUSTOMER, null);
        MvcResult prodResult = mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Api-Key", productionKey.rawKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodWalletRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse prodWallet = extractWalletResponse(prodResult);
        assertThat(prodWallet.environment()).isEqualTo(Wallet.Environment.PRODUCTION);

        mockMvc.perform(get("/api/v1/wallets/" + prodWallet.id())
                        .header("X-Api-Key", sandboxKey.rawKey()))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_reject_production_api_key_accessing_sandbox_wallet() throws Exception {
        AuthResponse auth = registerAndGetAuth("env-cross2@example.com", "Env Cross Org 2");

        ApiKeyResponse sandboxKey = createApiKey(auth, "Sandbox Key", "SANDBOX");
        ApiKeyResponse productionKey = createApiKey(auth, "Production Key", "PRODUCTION");

        CreateWalletRequest sandboxWalletRequest = new CreateWalletRequest("Sandbox Wallet", Wallet.Type.CUSTOMER, null);
        MvcResult sandboxResult = mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Api-Key", sandboxKey.rawKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sandboxWalletRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        WalletResponse sandboxWallet = extractWalletResponse(sandboxResult);
        assertThat(sandboxWallet.environment()).isEqualTo(Wallet.Environment.SANDBOX);

        mockMvc.perform(get("/api/v1/wallets/" + sandboxWallet.id())
                        .header("X-Api-Key", productionKey.rawKey()))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_filter_wallets_by_api_key_environment() throws Exception {
        AuthResponse auth = registerAndGetAuth("env-filter@example.com", "Env Filter Org");

        ApiKeyResponse sandboxKey = createApiKey(auth, "Sandbox Key", "SANDBOX");
        ApiKeyResponse productionKey = createApiKey(auth, "Production Key", "PRODUCTION");

        CreateWalletRequest sandboxReq = new CreateWalletRequest("Sandbox Wallet", Wallet.Type.CUSTOMER, null);
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Api-Key", sandboxKey.rawKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sandboxReq)))
                .andExpect(status().isCreated());

        CreateWalletRequest prodReq = new CreateWalletRequest("Prod Wallet", Wallet.Type.CUSTOMER, null);
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Api-Key", productionKey.rawKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/wallets")
                        .header("X-Api-Key", sandboxKey.rawKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].environment").value("SANDBOX"));

        mockMvc.perform(get("/api/v1/wallets")
                        .header("X-Api-Key", productionKey.rawKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].environment").value("PRODUCTION"));
    }

    @Test
    void should_allow_jwt_auth_to_see_all_environments() throws Exception {
        AuthResponse auth = registerAndGetAuth("env-all@example.com", "Env All Org");

        ApiKeyResponse sandboxKey = createApiKey(auth, "Sandbox Key", "SANDBOX");
        ApiKeyResponse productionKey = createApiKey(auth, "Production Key", "PRODUCTION");

        CreateWalletRequest sandboxReq = new CreateWalletRequest("Sandbox Wallet", Wallet.Type.CUSTOMER, null);
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Api-Key", sandboxKey.rawKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sandboxReq)))
                .andExpect(status().isCreated());

        CreateWalletRequest prodReq = new CreateWalletRequest("Prod Wallet", Wallet.Type.CUSTOMER, null);
        mockMvc.perform(post("/api/v1/wallets")
                        .header("X-Api-Key", productionKey.rawKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/wallets")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }
}
