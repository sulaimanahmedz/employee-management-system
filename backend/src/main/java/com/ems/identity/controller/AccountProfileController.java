package com.ems.identity.controller;

import com.ems.identity.dto.AccountIdentity;
import com.ems.identity.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountProfileController {
    private final AccountService accounts;
    public AccountProfileController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/api/v1/auth/profile")
    public AccountIdentity profile(Authentication authentication) {
        return accounts.findIdentity(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
