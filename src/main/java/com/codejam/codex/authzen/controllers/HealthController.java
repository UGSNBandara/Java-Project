package com.codejam.codex.authzen.controllers;

import com.codejam.codex.authzen.constants.ApiEndpoint;
import com.codejam.codex.authzen.responses.AuthzenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping(ApiEndpoint.HEALTH)
    public ResponseEntity<AuthzenResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthStatus = new HashMap<>();

        healthStatus.put("status", "UP");
        healthStatus.put("application", "AuthZen API");
        healthStatus.put("version", "1.0.0");
        healthStatus.put("timestamp", Instant.now().toString());
        healthStatus.put("uptime", getUptime());

        AuthzenResponse<Map<String, Object>> response = new AuthzenResponse<>(healthStatus);
        response.setMessage("Health check successful");

        return ResponseEntity.ok(response);
    }

    private String getUptime() {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptimeMillis / 1000 % 60;
        long minutes = uptimeMillis / (1000 * 60) % 60;
        long hours = uptimeMillis / (1000 * 60 * 60);

        return String.format("%02dh:%02dm:%02ds", hours, minutes, seconds);
    }
}
