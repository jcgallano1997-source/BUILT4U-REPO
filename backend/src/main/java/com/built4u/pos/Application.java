package com.built4u.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Built4U POS — single-business point-of-sale backend.
 *
 * <p>De-tenanted fork of FreePOS: there is one implicit business, so the
 * ENTITY/tenant layer is removed and {@code site_id} is the top isolation key.
 * Runs in its own Oracle schema ({@code BUILT4U}), fully isolated from the
 * live FreePOS schema ({@code FREEPOS}).
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
