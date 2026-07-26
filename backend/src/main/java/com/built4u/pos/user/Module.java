package com.built4u.pos.user;

import jakarta.persistence.*;
import lombok.*;

/**
 * Catalog of permissionable modules. Natural PK = {@code code}. Rows are seeded
 * by Flyway and read-only at runtime.
 */
@Entity
@Table(name = "pos_module")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
