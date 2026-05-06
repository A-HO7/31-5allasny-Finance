package com.team31.financetracker.reporting;

import com.team31.financetracker.reporting.config.JwtService;

public class TokenGen {
    public static void main(String[] args) {
        JwtService jwtService = new JwtService();
        System.out.println("TOKEN_START");
        System.out.println("TOKEN_END");
    }
}
