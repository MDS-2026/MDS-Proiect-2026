package com.mdsproject.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdsproject.backend.dto.wallet.CreateWalletRequest;
import com.mdsproject.backend.repositories.WalletRepository;
import com.mdsproject.backend.repositories.FairPayGroupRepository;
import com.mdsproject.backend.models.FairPayGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser
public class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private FairPayGroupRepository fairPayGroupRepository;

    @Autowired
    private ObjectMapper objectMapper;

   

    @Test
    void createWallet_shouldSucceed() throws Exception {
    // Creează un grup de test și îl salvează în DB
    FairPayGroup group = new FairPayGroup();
    group.setName("JUnit Test Group");
    group.setInviteCode(UUID.randomUUID().toString());
    fairPayGroupRepository.save(group);

    CreateWalletRequest req = new CreateWalletRequest();
    req.setName("JUnit Wallet");
    req.setPurpose("JUnit Test Purpose");
    req.setBudgetLimit(1000.0);
    req.setAutoApproveThreshold(100.0);
    req.setParentWalletId(null);

    String json = objectMapper.writeValueAsString(req);

    // Folosește ID-ul real al grupului
    UUID groupId = group.getId();

    mockMvc.perform(post("/api/groups/" + groupId + "/wallets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isOk());
    }
    @Test
    void createWallet_shouldSucceed_simple() throws Exception {
    FairPayGroup group = new FairPayGroup();
    group.setName("JUnit Test Group 2");
    group.setInviteCode(UUID.randomUUID().toString());
    fairPayGroupRepository.save(group);

    CreateWalletRequest req = new CreateWalletRequest();
    req.setName("JUnit Wallet");
    req.setPurpose("JUnit Test Purpose");
    req.setBudgetLimit(1000.0);
    req.setAutoApproveThreshold(100.0);
    req.setParentWalletId(null);

    String json = objectMapper.writeValueAsString(req);
    UUID groupId = group.getId();

    mockMvc.perform(post("/api/groups/" + groupId + "/wallets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isOk());
    }

    @Test
    void createWallet_negativeBudget_shouldFail() throws Exception {
    FairPayGroup group = new FairPayGroup();
    group.setName("JUnit Test Group 3");
    group.setInviteCode(UUID.randomUUID().toString());
    fairPayGroupRepository.save(group);

    CreateWalletRequest req = new CreateWalletRequest();
    req.setName("Negative Budget");
    req.setPurpose("Test");
    req.setBudgetLimit(-100.0);
    req.setAutoApproveThreshold(10.0);
    req.setParentWalletId(null);

    String json = objectMapper.writeValueAsString(req);
    UUID groupId = group.getId();

    mockMvc.perform(post("/api/groups/" + groupId + "/wallets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isBadRequest());
    }

    @Test
    void createWallet_negativeThreshold_shouldFail() throws Exception {
    FairPayGroup group = new FairPayGroup();
    group.setName("JUnit Test Group 4");
    group.setInviteCode(UUID.randomUUID().toString());
    fairPayGroupRepository.save(group);

    CreateWalletRequest req = new CreateWalletRequest();
    req.setName("Negative Threshold");
    req.setPurpose("Test");
    req.setBudgetLimit(100.0);
    req.setAutoApproveThreshold(-10.0);
    req.setParentWalletId(null);

    String json = objectMapper.writeValueAsString(req);
    UUID groupId = group.getId();

    mockMvc.perform(post("/api/groups/" + groupId + "/wallets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isBadRequest());
    }

    @Test
    void createWallet_missingName_shouldFail() throws Exception {
    FairPayGroup group = new FairPayGroup();
    group.setName("JUnit Test Group 5");
    group.setInviteCode(UUID.randomUUID().toString());
    fairPayGroupRepository.save(group);

    CreateWalletRequest req = new CreateWalletRequest();
    req.setName(""); // Empty name
    req.setPurpose("Test");
    req.setBudgetLimit(100.0);
    req.setAutoApproveThreshold(10.0);
    req.setParentWalletId(null);

    String json = objectMapper.writeValueAsString(req);
    UUID groupId = group.getId();

    mockMvc.perform(post("/api/groups/" + groupId + "/wallets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isBadRequest());
    }

    @Test
    void createWallet_missingBudget_shouldFail() throws Exception {
    FairPayGroup group = new FairPayGroup();
    group.setName("JUnit Test Group 6");
    group.setInviteCode(UUID.randomUUID().toString());
    fairPayGroupRepository.save(group);

    CreateWalletRequest req = new CreateWalletRequest();
    req.setName("No Budget");
    req.setPurpose("Test");
    // req.setBudgetLimit(null); // If field is primitive double, set to 0
    req.setAutoApproveThreshold(10.0);
    req.setParentWalletId(null);

    String json = objectMapper.writeValueAsString(req);
    UUID groupId = group.getId();

    mockMvc.perform(post("/api/groups/" + groupId + "/wallets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isBadRequest());
    }
}