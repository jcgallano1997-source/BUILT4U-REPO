package com.built4u.pos.uom;

public record UomDto(String uom, boolean active) {
    public static UomDto from(Uom u) {
        return new UomDto(u.getUom(), Boolean.TRUE.equals(u.getActive()));
    }
}
