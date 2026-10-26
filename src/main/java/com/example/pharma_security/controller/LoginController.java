package com.example.pharma_security.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping({"/", "/home"})
    public String home() { return "home"; }

    @GetMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied() { return "access-denied"; }
}
