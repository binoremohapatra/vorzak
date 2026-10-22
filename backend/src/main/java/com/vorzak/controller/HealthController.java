package com.vorzak.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "vorzak-backend",
                "version", "0.1.0",
                "timestamp", Instant.now().toString(),
                "disclaimer", "Integrity-checked evidence log. Not a legal certification and not proof of authenticity."
        ));
    }
}
