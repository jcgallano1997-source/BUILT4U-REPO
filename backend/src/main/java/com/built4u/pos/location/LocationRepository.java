package com.built4u.pos.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, LocationId> {

    Optional<Location> findBySiteIdAndLocId(Long siteId, Long locId);

    List<Location> findBySiteIdOrderByLocationAsc(Long siteId);

    @Query("""
           SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END
           FROM Location l
           WHERE l.siteId = :siteId
             AND LOWER(l.location) = LOWER(:name)
             AND (:excludeId IS NULL OR l.locId <> :excludeId)
           """)
    boolean existsByName(@Param("siteId") Long siteId,
                         @Param("name") String name,
                         @Param("excludeId") Long excludeId);
}
