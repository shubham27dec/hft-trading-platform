package com.hft.orderentry.controller;

import com.hft.orderentry.dto.RegistrationRequest;
import com.hft.orderentry.dto.RegistrationResponse;
import com.hft.orderentry.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest req) {
        try {
            String accountId = registrationService.register(req);
            return ResponseEntity.ok(new RegistrationResponse(accountId, req.username()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Registration failed — please try again"));
        }
    }
}
