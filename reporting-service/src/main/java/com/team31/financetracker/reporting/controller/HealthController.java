package com.team31.financetracker.reporting.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Ok";
    }

}
