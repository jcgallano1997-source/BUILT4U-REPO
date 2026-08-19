package com.built4u.pos.reportemail;

import java.io.Serializable;
import java.util.Objects;

/** Composite key for {@link ReportEmailRecipient}: (site_id, report_code, user_id). */
public class ReportEmailRecipientId implements Serializable {

    private Long siteId;
    private String reportCode;
    private Long userId;

    public ReportEmailRecipientId() {}

    public ReportEmailRecipientId(Long siteId, String reportCode, Long userId) {
        this.siteId = siteId;
        this.reportCode = reportCode;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportEmailRecipientId that)) return false;
        return Objects.equals(siteId, that.siteId)
            && Objects.equals(reportCode, that.reportCode)
            && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, reportCode, userId);
    }
}
