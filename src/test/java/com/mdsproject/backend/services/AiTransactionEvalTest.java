package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.transaction.CreateTransactionRequest;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.repositories.FairPayGroupRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AiTransactionEvalTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private FairPayGroupRepository groupRepository;

    private Wallet createWallet(String name, String purpose) {
        FairPayGroup group = new FairPayGroup();
        group.setName(name + " Group");
        group.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        group = groupRepository.save(group);

        Wallet wallet = new Wallet();
        wallet.setName(name);
        wallet.setPurpose(purpose);
        wallet.setGroup(group);
        wallet.setAutoApproveThreshold(100.0); // valoare default
        wallet.setBudgetLimit(1000.0); // valoare default
        return walletRepository.save(wallet);
    }

    @Test
    @WithMockUser(username = "user2@test.com", roles = {"USER"})
    @DisplayName("AI Transaction Validation Edge Cases")
    void testAiTransactionValidationEdgeCases() {
        Object[][] cases = new Object[][]{
                {"Travel", "Supermarket", false},
                {"Food/Restaurant", "Electronics", false},
                {"Office", "Entertainment", false},
                {"Travel", "Flight", true},
                {"Groceries", "Supermarket", true},
                {"Food/Restaurant", "Restaurant", true},
                {"Office", "Office Supplies", true},
                {"Travel", "Travel Insurance", true},
                {"Groceries", "Electronics", false},
                {"Entertainment", "Cinema", true},
        };
        for (Object[] testCase : cases) {
            String walletPurpose = (String) testCase[0];
            String category = (String) testCase[1];
            boolean expectedValid = (boolean) testCase[2];

            Wallet wallet = createWallet(walletPurpose, walletPurpose);

            com.mdsproject.backend.dto.transaction.ValidateTransactionRequest req =
                    new com.mdsproject.backend.dto.transaction.ValidateTransactionRequest();
            req.setWalletId(wallet.getId());
            req.setMerchant("Test");
            req.setCategory(category);
            req.setAmount(100.0);

            var response = transactionService.validateTransaction(req, "user2@test.com");
        boolean valid = response.valid();
        assertThat(valid)
            .withFailMessage("Expected valid=%s for wallet='%s', category='%s' (reason: %s)", expectedValid, walletPurpose, category, response.reason())
            .isEqualTo(expectedValid);
        }
    }
}
