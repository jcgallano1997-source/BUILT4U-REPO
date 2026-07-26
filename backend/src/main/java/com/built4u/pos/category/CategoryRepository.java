package com.built4u.pos.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, CategoryId> {

    Optional<Category> findBySiteIdAndCatId(Long siteId, Long catId);

    List<Category> findBySiteIdOrderByCategoryNameAsc(Long siteId);

    @Query("""
           SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
           FROM Category c
           WHERE c.siteId = :siteId
             AND LOWER(c.categoryName) = LOWER(:name)
             AND (:excludeId IS NULL OR c.catId <> :excludeId)
           """)
    boolean existsByName(@Param("siteId") Long siteId,
                         @Param("name") String name,
                         @Param("excludeId") Long excludeId);
}
