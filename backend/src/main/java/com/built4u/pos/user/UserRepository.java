package com.built4u.pos.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    java.util.List<User> findAllByOrderByUsernameAsc();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** How many users are assigned the given role — blocks deleting an in-use role. */
    long countByRoles_Id(Long roleId);

    /**
     * Count of OTHER active users that hold a wildcard (ADMIN) role — the
     * last-administrator guard. Excludes the user being edited.
     */
    @Query("""
           SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r
           WHERE u.active = 'Y' AND r.wildcard = 'Y' AND u.id <> :excludeUserId
           """)
    long countOtherActiveAdmins(@Param("excludeUserId") Long excludeUserId);
}
