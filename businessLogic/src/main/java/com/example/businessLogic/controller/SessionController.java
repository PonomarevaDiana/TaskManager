package com.example.businessLogic.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/create-session")
public class SessionController {
    @PostMapping
    public ResponseEntity<Void> createSession() {
        return ResponseEntity.ok().build();
    }
}
