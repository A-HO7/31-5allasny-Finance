package com.team31.financetracker.budget.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

}

