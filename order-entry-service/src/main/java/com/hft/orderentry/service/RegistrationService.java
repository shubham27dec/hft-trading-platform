package com.hft.orderentry.service;

import com.hft.orderentry.dto.RegistrationRequest;
import com.hft.orderentry.entity.TraderAccount;
import com.hft.orderentry.repository.TraderAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RegistrationService {

    private final RestClient restClient;
    private final TraderAccountRepository traderAccountRepository;
    private final String keycloakBaseUrl;
    private final String adminUsername;
    private final String adminPassword;

    public RegistrationService(
            RestClient.Builder builder,
            TraderAccountRepository traderAccountRepository,
            @Value("${keycloak.admin.base-url:http://localhost:8180}") String keycloakBaseUrl,
            @Value("${keycloak.admin.username:admin}") String adminUsername,
            @Value("${keycloak.admin.password:admin}") String adminPassword) {
        this.restClient = builder.build();
        this.traderAccountRepository = traderAccountRepository;
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public String register(RegistrationRequest req) {
        if (traderAccountRepository.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        String adminToken = getAdminToken();
        String userId = createKeycloakUser(adminToken, req);
        createTraderRow(userId, req.username());
        log.info("Registered new trader: {} ({})", req.username(), userId);
        return userId;
    }

    @SuppressWarnings("unchecked")
    private String getAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", "admin-cli");
        form.add("grant_type", "password");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        Map<String, Object> resp = restClient.post()
                .uri(keycloakBaseUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (resp == null || !resp.containsKey("access_token")) {
            throw new IllegalStateException("Could not obtain Keycloak admin token");
        }
        return (String) resp.get("access_token");
    }

    private String createKeycloakUser(String adminToken, RegistrationRequest req) {
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", req.password(),
                "temporary", false
        );
        Map<String, Object> user = Map.of(
                "username", req.username(),
                "email", req.email(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(credential)
        );

        var response = restClient.post()
                .uri(keycloakBaseUrl + "/admin/realms/hft/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .toBodilessEntity();

        String location = response.getHeaders().getFirst("Location");
        if (location == null) {
            throw new IllegalStateException("Keycloak did not return user location");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void createTraderRow(String accountId, String username) {
        TraderAccount account = new TraderAccount();
        account.setAccountId(accountId);
        account.setUsername(username);
        account.setApiKey("svc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        account.setBuyingPower(100_000.0);
        account.setMarginLimit(50_000.0);
        traderAccountRepository.save(account);
    }
}
