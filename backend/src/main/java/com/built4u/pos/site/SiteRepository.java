package com.built4u.pos.site;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    Optional<Site> findByCode(String code);

    boolean existsByCode(String code);

    List<Site> findAllByOrderByCodeAsc();

    @Query("SELECT COUNT(s) FROM Site s WHERE s.active = 'Y'")
    long countActive();

    /** Count of users currently assigned to this site (regardless of user-active state). */
    @Query("SELECT COUNT(u) FROM User u JOIN u.sites s WHERE s.id = :siteId")
    long countUsers(@Param("siteId") Long siteId);
}
