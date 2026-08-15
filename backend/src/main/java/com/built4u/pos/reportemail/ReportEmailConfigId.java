package com.built4u.pos.reportemail;

import java.io.Serializable;
import java.util.Objects;

/** Composite key for {@link ReportEmailConfig}: (site_id, report_code). */
public class ReportEmailConfigId implements Serializable {

    private Long siteId;
    private String reportCode;

    public ReportEmailConfigId() {}

    public ReportEmailConfigId(Long siteId, String reportCode) {
        this.siteId = siteId;
        this.reportCode = reportCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportEmailConfigId that)) return false;
        return Objects.equals(siteId, that.siteId) && Objects.equals(reportCode, that.reportCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, reportCode);
    }
}
