package com.argent.integration;

import com.argent.module.auth.dto.ApiKeyResponse;
import com.argent.module.auth.dto.AuthResponse;
import com.argent.module.auth.dto.CreateApiKeyRequest;
import com.argent.module.auth.dto.LoginRequest;
import com.argent.module.auth.dto.RegisterRequest;
import com.argent.module.auth.entity.ApiKey;
import com.argent.module.auth.entity.User;
import com.argent.module.auth.repository.UserRepository;
import com.argent.module.organization.entity.Organization;
import com.argent.module.organization.repository.OrganizationRepository;
import com.argent.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    private ApiKeyResponse extractApiKeyResponse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        ApiResponse<ApiKeyResponse> response = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ApiKeyResponse.class));
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
    void should_create_organization_and_generate_api_key() throws Exception {
        AuthResponse auth = registerAndGetAuth("orgowner@example.com", "My Org");

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.argent.module.organization.dto.CreateOrganizationRequest("Second Org", "second-org"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Second Org"));

        CreateApiKeyRequest apiKeyRequest = new CreateApiKeyRequest("Production Key", ApiKey.Environment.PRODUCTION);
        mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiKeyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Production Key"))
                .andExpect(jsonPath("$.data.rawKey").isNotEmpty());
    }

    @Test
    void should_revoke_api_key() throws Exception {
        AuthResponse auth = registerAndGetAuth("revoke@example.com", "Revoke Org");

        CreateApiKeyRequest apiKeyRequest = new CreateApiKeyRequest("Key to Revoke", ApiKey.Environment.SANDBOX);
        MvcResult result = mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiKeyRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiKeyResponse keyResponse = extractApiKeyResponse(result);

        mockMvc.perform(delete("/api/v1/api-keys/" + keyResponse.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_list_api_keys() throws Exception {
        AuthResponse auth = registerAndGetAuth("listkeys@example.com", "List Org");

        CreateApiKeyRequest request1 = new CreateApiKeyRequest("Key 1", ApiKey.Environment.SANDBOX);
        CreateApiKeyRequest request2 = new CreateApiKeyRequest("Key 2", ApiKey.Environment.PRODUCTION);

        mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void should_prevent_developer_from_managing_api_keys() throws Exception {
        AuthResponse ownerAuth = registerAndGetAuth("devowner@example.com", "Dev Org");

        Organization org = organizationRepository.findById(ownerAuth.user().organizationId()).orElseThrow();

        User developer = User.builder()
                .organization(org)
                .email("developer@example.com")
                .name("Dev User")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.DEVELOPER)
                .build();
        userRepository.save(developer);

        LoginRequest devLogin = new LoginRequest("developer@example.com", "password123");
        MvcResult devResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(devLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse devAuth = extractAuthResponse(devResult);

        CreateApiKeyRequest apiKeyRequest = new CreateApiKeyRequest("Dev Key", ApiKey.Environment.SANDBOX);
        mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + devAuth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiKeyRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_add_member_as_owner() throws Exception {
        AuthResponse ownerAuth = registerAndGetAuth("ownermember@example.com", "Member Org");

        String body = objectMapper.writeValueAsString(new com.argent.module.organization.dto.AddMemberRequest("newdev@test.com", User.Role.DEVELOPER, "password123"));
        mockMvc.perform(post("/api/v1/organizations/" + ownerAuth.user().organizationId() + "/members")
                        .header("Authorization", "Bearer " + ownerAuth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newdev@test.com"))
                .andExpect(jsonPath("$.data.role").value("DEVELOPER"));

        LoginRequest devLogin = new LoginRequest("newdev@test.com", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(devLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.data.user.organizationId").value(ownerAuth.user().organizationId().toString()));
    }

    @Test
    void should_reject_developer_from_adding_members() throws Exception {
        AuthResponse ownerAuth = registerAndGetAuth("ownerdev2@example.com", "Dev Member Org");

        User devUser = User.builder()
                .organization(organizationRepository.findById(ownerAuth.user().organizationId()).orElseThrow())
                .email("devmember@test.com")
                .name("Dev Member")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.DEVELOPER)
                .build();
        userRepository.save(devUser);

        LoginRequest devLogin = new LoginRequest("devmember@test.com", "password123");
        MvcResult devResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(devLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse devAuth = extractAuthResponse(devResult);

        String body = objectMapper.writeValueAsString(new com.argent.module.organization.dto.AddMemberRequest("another@test.com", User.Role.DEVELOPER, "password123"));
        mockMvc.perform(post("/api/v1/organizations/" + ownerAuth.user().organizationId() + "/members")
                        .header("Authorization", "Bearer " + devAuth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
