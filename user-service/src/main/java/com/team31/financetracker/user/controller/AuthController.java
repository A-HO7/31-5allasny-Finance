package com.team31.financetracker.user.controller;

import com.team31.financetracker.user.dto.RegisterRequest;
import com.team31.financetracker.user.service.UserService;
import com.team31.financetracker.user.config.JwtConfigurationManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@RequestBody RegisterRequest request) {
        String token = userService.registerUser(request);
        return Map.of(
                "token", token,
                "expiresIn", String.valueOf(JwtConfigurationManager.getInstance().getExpirationMs())
        );
    }
}