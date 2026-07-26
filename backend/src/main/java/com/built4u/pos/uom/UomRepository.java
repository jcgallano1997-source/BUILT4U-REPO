package com.built4u.pos.uom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UomRepository extends JpaRepository<Uom, UomId> {

    Optional<Uom> findBySiteIdAndUom(Long siteId, String uom);

    List<Uom> findBySiteIdOrderByUomAsc(Long siteId);

    List<Uom> findBySiteIdAndActiveTrueOrderByUomAsc(Long siteId);
}
