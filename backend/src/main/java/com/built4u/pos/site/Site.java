package com.built4u.pos.site;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A branch/store of the single business. {@code site_id} is the top data-
 * isolation key across all business tables (no tenant/entity layer above it).
 * Site {@code code} is globally unique for the business.
 */
@Entity
@Table(name = "pos_site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(nullable = false, length = 1)
    private String active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (active == null) active = "Y";
    }

    public boolean isActive() {
        return "Y".equals(active);
    }
}
