package com.built4u.pos.sale;

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
public class ReturnItemId implements Serializable {
    private Long siteId;
    private String returnNumber;
    private Long itemId;
}
