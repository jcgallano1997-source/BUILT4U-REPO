package com.built4u.pos.location;

import java.math.BigDecimal;

public record LocationDto(Long id, String name, BigDecimal capacity, boolean active) {
    public static LocationDto from(Location l) {
        return new LocationDto(l.getLocId(), l.getLocation(), l.getCapacity(), Boolean.TRUE.equals(l.getActive()));
    }
}
