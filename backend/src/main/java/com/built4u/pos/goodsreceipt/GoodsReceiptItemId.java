package com.built4u.pos.goodsreceipt;

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
public class GoodsReceiptItemId implements Serializable {
    private Long siteId;
    private String grNumber;
    private Long itemId;
}
