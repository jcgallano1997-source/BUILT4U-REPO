package com.built4u.pos.item;

import com.built4u.pos.category.Category;
import com.built4u.pos.category.CategoryRepository;
import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.tenant.TenantContext;
import com.built4u.pos.item.dto.ImportResultDto;
import com.built4u.pos.location.Location;
import com.built4u.pos.location.LocationRepository;
import com.built4u.pos.uom.UomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bulk-imports inventory from an .xlsx spreadsheet. Expected header row
 * (case-insensitive, any order): code, name, category, location, uom, quantity,
 * sellingPrice, costPrice. Category / location / uom must already exist at the
 * site (rows referencing unknown lookups are reported as errors, not created).
 * An item is matched by code — updated in place, or created if new. The whole
 * import is one transaction: any thrown error rolls it all back.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryImportService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final UomRepository uomRepository;

    @Transactional
    public ImportResultDto importXlsx(MultipartFile file) {
        Long siteId = TenantContext.requireSiteId();
        if (file == null || file.isEmpty()) throw new BadRequestException("No file uploaded.");

        Map<String, Category> catByName = new HashMap<>();
        for (Category c : categoryRepository.findBySiteIdOrderByCategoryNameAsc(siteId)) {
            catByName.put(c.getCategoryName().toLowerCase(), c);
        }
        Map<String, Location> locByName = new HashMap<>();
        for (Location l : locationRepository.findBySiteIdOrderByLocationAsc(siteId)) {
            locByName.put(l.getLocation().toLowerCase(), l);
        }

        int created = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new BadRequestException("The spreadsheet has no sheets.");
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new BadRequestException("The spreadsheet is empty.");
            Map<String, Integer> col = headerIndex(header);
            requireCols(col, "code", "name", "category", "location", "uom");

            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String code = str(row, col.get("code"));
                if (code == null || code.isBlank()) { skipped++; continue; }
                int line = r + 1;
                try {
                    String name = str(row, col.get("name"));
                    if (name == null || name.isBlank()) { errors.add("Row " + line + ": name is required"); continue; }
                    Category cat = catByName.get(low(str(row, col.get("category"))));
                    if (cat == null) { errors.add("Row " + line + ": unknown category '" + str(row, col.get("category")) + "'"); continue; }
                    Location loc = locByName.get(low(str(row, col.get("location"))));
                    if (loc == null) { errors.add("Row " + line + ": unknown location '" + str(row, col.get("location")) + "'"); continue; }
                    String uom = str(row, col.get("uom"));
                    if (uom == null || uomRepository.findBySiteIdAndUom(siteId, uom.trim()).isEmpty()) {
                        errors.add("Row " + line + ": unknown unit '" + uom + "'"); continue;
                    }
                    BigDecimal qty = num(row, col.get("quantity"));
                    BigDecimal sell = num(row, col.get("sellingPrice"));
                    BigDecimal cost = num(row, col.get("costPrice"));

                    var existing = itemRepository.findBySiteIdAndItemCodeIgnoreCase(siteId, code.trim());
                    if (existing.isPresent()) {
                        Item it = existing.get();
                        it.setItemName(name.trim());
                        it.setCatId(cat.getCatId());
                        it.setLocId(loc.getLocId());
                        it.setUom(uom.trim());
                        if (qty != null) it.setQuantity(qty);
                        if (sell != null) it.setSellingPrice(sell);
                        if (cost != null) it.setCostPrice(cost);
                        itemRepository.save(it);
                        updated++;
                    } else {
                        itemRepository.save(Item.builder()
                            .siteId(siteId).catId(cat.getCatId()).locId(loc.getLocId())
                            .itemCode(code.trim()).itemName(name.trim()).uom(uom.trim())
                            .quantity(qty == null ? BigDecimal.ZERO : qty)
                            .sellingPrice(sell == null ? BigDecimal.ZERO : sell)
                            .costPrice(cost == null ? BigDecimal.ZERO : cost)
                            .active(true).build());
                        created++;
                    }
                } catch (Exception e) {
                    errors.add("Row " + line + ": " + e.getMessage());
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Could not read the spreadsheet: " + e.getMessage());
        }

        log.info("Inventory import: {} created, {} updated, {} skipped, {} error(s)",
            created, updated, skipped, errors.size());
        return new ImportResultDto(created, updated, skipped, errors);
    }

    private static Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> col = new HashMap<>();
        for (int i = header.getFirstCellNum(); i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c != null && c.getCellType() == CellType.STRING) {
                col.put(c.getStringCellValue().trim().toLowerCase().replace(" ", ""), i);
            }
        }
        return col;
    }

    private static void requireCols(Map<String, Integer> col, String... names) {
        for (String n : names) {
            if (!col.containsKey(n.toLowerCase())) {
                throw new BadRequestException("Missing required column '" + n + "'.");
            }
        }
    }

    private static String str(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(c.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> null;
        };
    }

    private static BigDecimal num(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(c.getNumericCellValue());
        if (c.getCellType() == CellType.STRING) {
            String s = c.getStringCellValue().trim();
            if (s.isEmpty()) return null;
            try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static String low(String s) { return s == null ? null : s.trim().toLowerCase(); }
}
