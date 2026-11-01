package org.nikahgator.web;

import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final JdbcClient jdbc;

    public PublicController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** 200 only when the backend and its database are up (used by deploy.sh). */
    @GetMapping("/health")
    public Map<String, String> health() {
        jdbc.sql("select 1").query(Integer.class).single();
        return Map.of("status", "UP");
    }
}
