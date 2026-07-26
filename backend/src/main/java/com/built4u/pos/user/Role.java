package com.built4u.pos.user;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pos_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    /** 'Y' for the three seeded roles (ADMIN/MANAGER/CASHIER) — cannot be deleted. */
    @Column(name = "built_in", nullable = false, length = 1)
    @Builder.Default
    private String builtIn = "N";

    /** 'Y' only for ADMIN — implicitly holds every module (incl. future) and is immutable. */
    @Column(nullable = false, length = 1)
    @Builder.Default
    private String wildcard = "N";

    /** Module codes this role grants. Empty for the wildcard (ADMIN) role. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "pos_role_module",
        joinColumns = @JoinColumn(name = "role_id")
    )
    @Column(name = "module_code", length = 40)
    @Builder.Default
    private Set<String> moduleCodes = new HashSet<>();

    public boolean isBuiltIn() {
        return "Y".equals(builtIn);
    }

    public boolean isWildcard() {
        return "Y".equals(wildcard);
    }
}
