package integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kaesoron.wallet.Application;
import org.kaesoron.wallet.dto.WalletOperationRequest;
import org.kaesoron.wallet.enums.OperationType;
import org.kaesoron.wallet.model.Wallet;
import org.kaesoron.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class WalletIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("walletdb")
            .withUsername("wallet")
            .withPassword("wallet");
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WalletRepository walletRepository;
    private UUID walletId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setup() {
        walletId = UUID.randomUUID();
        walletRepository.save(new Wallet(walletId, 1000L));
    }

    @Test
    void testDepositAndGetBalance() throws Exception {
        var request = new WalletOperationRequest(walletId, OperationType.DEPOSIT, 500L);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/wallets/" + walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1500));
    }

    @Test
    void testWithdrawWithInsufficientFunds() throws Exception {
        var request = new WalletOperationRequest(walletId, OperationType.WITHDRAW, 2000L);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient Funds"));
    }

    @Test
    void testMalformedJson() throws Exception {
        String malformedJson = "{\"walletId\": \"" + walletId + "\", \"operationType\": \"DEPOSIT\""; // no closing brace

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON"));
    }

    @Test
    void testWalletNotFound() throws Exception {
        UUID nonexistentId = UUID.randomUUID();
        var request = new WalletOperationRequest(nonexistentId, OperationType.WITHDRAW, 100L);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet Not Found"));
    }

    @Test
    void testValidation_NegativeAmount() throws Exception {
        var request = new WalletOperationRequest(walletId, OperationType.DEPOSIT, -100L);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.messages[*].message", Matchers.hasItem(Matchers.containsString("must be greater than 0"))))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testValidation_NullOperationType() throws Exception {
        var request = new WalletOperationRequest(walletId, null, 100L);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.messages[*].message", Matchers.hasItem(Matchers.containsString("must not be null"))))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testUnsupportedOperationHandledGracefully() throws Exception {
        // Создаем json с невалидным operationType
        String invalidOperationJson = """
                {
                  "walletId": "%s",
                  "operationType": "TRANSFER",
                  "amount": 100
                }
                """.formatted(walletId);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidOperationJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON"))
                .andExpect(jsonPath("$.message", containsString("Cannot deserialize value of type")));
    }

    @Test
    void testErrorResponseFormatForInsufficientFunds() throws Exception {
        var request = new WalletOperationRequest(walletId, OperationType.WITHDRAW, 2000L);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient Funds"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(422));
    }
}
