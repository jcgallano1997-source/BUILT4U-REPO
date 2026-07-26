package com.built4u.pos.uom;

import com.built4u.pos.common.tenant.YesNoConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pos_uom")
@IdClass(UomId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Uom {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(nullable = false, length = 50)
    private String uom;

    @Column(nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean active = true;
}
