package com.built4u.pos;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/** Minimal liveness endpoint used to confirm the app booted (Phase 1 skeleton). */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "app", "built4u-pos",
            "status", "up",
            "time", OffsetDateTime.now().toString()
        );
    }
}
