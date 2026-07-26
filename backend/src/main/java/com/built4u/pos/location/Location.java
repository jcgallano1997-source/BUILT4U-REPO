package com.built4u.pos.location;

import com.built4u.pos.common.tenant.YesNoConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_location")
@IdClass(LocationId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Id
    @Column(name = "loc_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pos_location_seq")
    @SequenceGenerator(name = "pos_location_seq", sequenceName = "pos_location_seq", allocationSize = 1)
    private Long locId;

    @Column(nullable = false, length = 100)
    private String location;

    @Column
    private BigDecimal capacity;

    @Column(nullable = false, length = 1)
    @Convert(converter = YesNoConverter.class)
    @Builder.Default
    private Boolean active = true;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

    @LastModifiedBy
    @Column(name = "last_update_by", length = 50)
    private String lastUpdateBy;
}
