package com.built4u.pos.item.dto;

import java.util.List;

/** Outcome of an inventory spreadsheet import. */
public record ImportResultDto(
    int created,
    int updated,
    int skipped,
    List<String> errors
) {}
