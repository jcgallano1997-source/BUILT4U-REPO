package com.built4u.pos.category;

import com.built4u.pos.category.dto.CreateCategoryRequest;
import com.built4u.pos.category.dto.UpdateCategoryRequest;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> list(String search, boolean includeInactive) {
        Long siteId = TenantContext.requireSiteId();
        String needle = search == null ? "" : search.toLowerCase().trim();
        return categoryRepository.findBySiteIdOrderByCategoryNameAsc(siteId).stream()
            .filter(c -> includeInactive || Boolean.TRUE.equals(c.getActive()))
            .filter(c -> needle.isEmpty() || c.getCategoryName().toLowerCase().contains(needle))
            .map(CategoryDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto get(Long catId) {
        Long siteId = TenantContext.requireSiteId();
        return categoryRepository.findBySiteIdAndCatId(siteId, catId)
            .map(CategoryDto::from)
            .orElseThrow(() -> new NotFoundException("Category " + catId + " not found"));
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequest req) {
        Long siteId = TenantContext.requireSiteId();
        String name = req.name().trim();
        if (categoryRepository.existsByName(siteId, name, null)) {
            throw new ConflictException("Category '" + name + "' already exists at this site");
        }
        Category entity = Category.builder().siteId(siteId).categoryName(name).active(true).build();
        return CategoryDto.from(categoryRepository.save(entity));
    }

    @Transactional
    public CategoryDto update(Long catId, UpdateCategoryRequest req) {
        Long siteId = TenantContext.requireSiteId();
        Category c = categoryRepository.findBySiteIdAndCatId(siteId, catId)
            .orElseThrow(() -> new NotFoundException("Category " + catId + " not found"));
        String name = req.name().trim();
        if (categoryRepository.existsByName(siteId, name, catId)) {
            throw new ConflictException("Category '" + name + "' already exists at this site");
        }
        c.setCategoryName(name);
        c.setActive(Boolean.TRUE.equals(req.active()));
        return CategoryDto.from(categoryRepository.save(c));
    }

    @Transactional
    public void softDelete(Long catId) {
        Long siteId = TenantContext.requireSiteId();
        Category c = categoryRepository.findBySiteIdAndCatId(siteId, catId)
            .orElseThrow(() -> new NotFoundException("Category " + catId + " not found"));
        c.setActive(false);
        categoryRepository.save(c);
    }
}
