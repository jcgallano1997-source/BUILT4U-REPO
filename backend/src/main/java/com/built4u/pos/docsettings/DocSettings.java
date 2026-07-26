package com.built4u.pos.docsettings;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Per-site document/branding config ({@code pos_doc_settings}), keyed by site_id.
 * Consumed by the report-PDF letterhead and the sale-receipt PDF. No row for a
 * site ⇒ the service falls through to hard-coded defaults.
 */
@Entity
@Table(name = "pos_doc_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocSettings {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "contact_line", length = 150)
    private String contactLine;

    @Column(length = 40)
    private String tin;

    @Column(name = "footer_note", length = 255)
    private String footerNote;

    @Column(name = "accent_color", nullable = false, length = 7)
    @Builder.Default
    private String accentColor = "#1D4ED8";

    @Column(name = "receipt_title", nullable = false, length = 60)
    @Builder.Default
    private String receiptTitle = "SALES RECEIPT";

    @Column(name = "receipt_footer", nullable = false, length = 255)
    @Builder.Default
    private String receiptFooter = "Thank you!";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
