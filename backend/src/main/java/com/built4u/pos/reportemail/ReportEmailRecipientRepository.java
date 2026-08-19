package com.built4u.pos.reportemail;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Report → recipient users, scoped to a site. */
public interface ReportEmailRecipientRepository
    extends JpaRepository<ReportEmailRecipient, ReportEmailRecipientId> {

    List<ReportEmailRecipient> findBySiteIdAndReportCode(Long siteId, String reportCode);

    List<ReportEmailRecipient> findBySiteId(Long siteId);

    void deleteBySiteIdAndReportCode(Long siteId, String reportCode);
}
