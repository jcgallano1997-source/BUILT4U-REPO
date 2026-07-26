package com.built4u.pos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reports & exports against BUILT4U: a sale feeds the sales-overview JSON;
 * xlsx and pdf exports return well-formed binaries; inventory-snapshot lists
 * stock; and an xlsx upload bulk-imports items.
 * @Transactional rolls back. MOCK web env avoids the loopback issue.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ReportFlowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String auth;

    private JsonNode postJson(String url, String body) throws Exception {
        String res = mvc.perform(post(url).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }
    private JsonNode getJson(String url) throws Exception {
        String res = mvc.perform(get(url).header("Authorization", auth))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }
    private byte[] getBytes(String url, String contentType) throws Exception {
        return mvc.perform(get(url).header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(res -> assertThat(res.getResponse().getContentType()).contains(contentType))
            .andReturn().getResponse().getContentAsByteArray();
    }

    private void loginAsAdmin() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"admin123","siteCode":"MAIN"}"""))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        auth = "Bearer " + json.readTree(login).get("accessToken").asText();
    }

    @Test
    void salesAndInventoryReports_json_xlsx_pdf() throws Exception {
        loginAsAdmin();
        long catId = postJson("/api/categories", "{\"name\":\"RepCat\"}").get("id").asLong();
        long locId = postJson("/api/locations", "{\"name\":\"RepLoc\"}").get("id").asLong();
        postJson("/api/uoms", "{\"uom\":\"RPC\"}");
        long itemId = postJson("/api/items", """
            {"code":"RPT1","name":"Report Item","catId":%d,"locId":%d,"uom":"RPC","quantity":10,"sellingPrice":50.00}"""
            .formatted(catId, locId)).get("id").asLong();
        postJson("/api/shifts/open", "{\"openingFloat\":0}");
        postJson("/api/sales", """
            {"modeOfPayment":"CASH","payment":100.00,"lines":[{"itemId":%d,"quantity":2}]}""".formatted(itemId));

        // Sales overview JSON — one ₱100 sale today.
        String today = LocalDate.now().toString();
        JsonNode ov = getJson("/api/reports/sales-overview?from=" + today + "&to=" + today);
        assertThat(ov.get("salesCount").asLong()).isEqualTo(1);
        assertThat(ov.get("netSales").asDouble()).isEqualTo(100.0);
        assertThat(ov.get("byMode").get(0).get("mode").asText()).isEqualTo("CASH");

        // xlsx + pdf exports are well-formed binaries.
        byte[] xlsx = getBytes("/api/reports/sales-overview?from=" + today + "&to=" + today + "&format=xlsx",
            "spreadsheetml");
        assertThat(xlsx.length).isGreaterThan(100);
        assertThat(new String(xlsx, 0, 2)).isEqualTo("PK");   // xlsx = zip
        byte[] pdf = getBytes("/api/reports/inventory-valuation?format=pdf", "pdf");
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        // Inventory snapshot lists the item.
        JsonNode snap = getJson("/api/reports/inventory-snapshot");
        assertThat(snap.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void inventoryImport_createsAndUpdatesItems() throws Exception {
        loginAsAdmin();
        postJson("/api/categories", "{\"name\":\"ImpCat\"}");
        postJson("/api/locations", "{\"name\":\"ImpLoc\"}");
        postJson("/api/uoms", "{\"uom\":\"IMP\"}");

        byte[] wb = buildSheet(new String[][]{
            {"code", "name", "category", "location", "uom", "quantity", "sellingPrice", "costPrice"},
            {"IMP-1", "Imported One", "ImpCat", "ImpLoc", "IMP", "7", "12.50", "8.00"},
            {"IMP-2", "Imported Two", "ImpCat", "ImpLoc", "IMP", "3", "5.00", "2.00"},
            {"IMP-3", "Bad Cat", "NopeCat", "ImpLoc", "IMP", "1", "1.00", "1.00"},
        });
        var file = new MockMultipartFile("file", "items.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", wb);

        String res = mvc.perform(multipart("/api/items/import").file(file).header("Authorization", auth))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode r = json.readTree(res);
        assertThat(r.get("created").asInt()).isEqualTo(2);
        assertThat(r.get("errors").size()).isEqualTo(1);   // the unknown-category row

        // The imported item is now searchable with the right quantity.
        JsonNode found = getJson("/api/items?search=IMP-1");
        assertThat(found.get(0).get("quantity").asDouble()).isEqualTo(7.0);
    }

    private static byte[] buildSheet(String[][] cells) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Items");
            for (int r = 0; r < cells.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < cells[r].length; c++) row.createCell(c).setCellValue(cells[r][c]);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
