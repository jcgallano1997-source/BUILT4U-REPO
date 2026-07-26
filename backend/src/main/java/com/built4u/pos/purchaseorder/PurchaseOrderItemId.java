package com.built4u.pos.purchaseorder;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PurchaseOrderItemId implements Serializable {
    private Long siteId;
    private String poNumber;
    private Long itemId;
}
