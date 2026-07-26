package com.built4u.pos.category;

public record CategoryDto(Long id, String name, boolean active) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getCatId(), c.getCategoryName(), Boolean.TRUE.equals(c.getActive()));
    }
}
