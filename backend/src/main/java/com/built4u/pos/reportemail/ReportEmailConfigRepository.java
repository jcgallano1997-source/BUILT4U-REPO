package com.built4u.pos.reportemail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportEmailConfigRepository extends JpaRepository<ReportEmailConfig, ReportEmailConfigId> {

    Optional<ReportEmailConfig> findBySiteIdAndReportCode(Long siteId, String reportCode);

    List<ReportEmailConfig> findBySiteIdOrderByReportCodeAsc(Long siteId);
}
