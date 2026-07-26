package com.built4u.pos.category;

import com.built4u.pos.category.dto.CreateCategoryRequest;
import com.built4u.pos.category.dto.UpdateCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    private static final String READ_ANY =
        "hasAnyAuthority('MOD_CATEGORIES','MOD_INVENTORY','MOD_STOCKTAKE'," +
        "'MOD_INVENTORY_SNAPSHOT','MOD_INVENTORY_VALUATION','MOD_INVENTORY_MOVEMENT')";

    @GetMapping
    @PreAuthorize(READ_ANY)
    public ResponseEntity<List<CategoryDto>> list(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive
    ) {
        return ResponseEntity.ok(categoryService.list(search, includeInactive));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ANY)
    public ResponseEntity<CategoryDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(categoryService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOD_CATEGORIES')")
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CreateCategoryRequest req) {
        return ResponseEntity.ok(categoryService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_CATEGORIES')")
    public ResponseEntity<CategoryDto> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateCategoryRequest req) {
        return ResponseEntity.ok(categoryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MOD_CATEGORIES')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        categoryService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
