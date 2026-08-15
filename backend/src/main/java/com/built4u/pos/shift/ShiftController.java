package com.built4u.pos.shift;

import com.built4u.pos.shift.dto.CashMovementDto;
import com.built4u.pos.shift.dto.CloseShiftRequest;
import com.built4u.pos.shift.dto.OpenShiftRequest;
import com.built4u.pos.shift.dto.RecordCashMovementRequest;
import com.built4u.pos.shift.dto.ShiftDto;
import com.built4u.pos.shift.dto.ShiftSummaryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping("/open")
    @PreAuthorize("hasAnyAuthority('MOD_SHIFTS','MOD_SHIFTS_ADMIN')")
    public ResponseEntity<ShiftDto> open(@Valid @RequestBody OpenShiftRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftService.openShift(req));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('MOD_SHIFTS','MOD_SHIFTS_ADMIN','MOD_POS')")
    public ResponseEntity<ShiftDto> current() {
        return ResponseEntity.ok(shiftService.getCurrentShift());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyAuthority('MOD_SHIFTS','MOD_SHIFTS_ADMIN')")
    public ResponseEntity<List<ShiftSummaryDto>> mine() {
        return ResponseEntity.ok(shiftService.listMyShifts());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MOD_SHIFTS_ADMIN')")
    public ResponseEntity<List<ShiftSummaryDto>> list() {
        return ResponseEntity.ok(shiftService.listShifts());
    }

    @PostMapping("/{shiftNumber}/close")
    @PreAuthorize("hasAnyAuthority('MOD_SHIFTS','MOD_SHIFTS_ADMIN')")
    public ResponseEntity<ShiftDto> close(@PathVariable("shiftNumber") String shiftNumber,
                                          @Valid @RequestBody CloseShiftRequest req) {
        return ResponseEntity.ok(shiftService.closeShift(shiftNumber, req));
    }

    @GetMapping("/{shiftNumber}")
    @PreAuthorize("hasAnyAuthority('MOD_SHIFTS','MOD_SHIFTS_ADMIN')")
    public ResponseEntity<ShiftDto> get(@PathVariable("shiftNumber") String shiftNumber) {
        return ResponseEntity.ok(shiftService.getShift(shiftNumber));
    }

    @PostMapping("/{shiftNumber}/cash-movement")
    @PreAuthorize("hasAnyAuthority('MOD_SHIFTS','MOD_SHIFTS_ADMIN','MOD_POS')")
    public ResponseEntity<ShiftDto> recordCashMovement(@PathVariable("shiftNumber") String shiftNumber,
                                                       @Valid @RequestBody RecordCashMovementRequest req) {
        return ResponseEntity.ok(shiftService.recordCashMovement(shiftNumber, req));
    }

    @GetMapping("/{shiftNumber}/cash-movements")
    @PreAuthorize("hasAnyAuthority('MOD_SHIFTS','MOD_SHIFTS_ADMIN','MOD_POS')")
    public ResponseEntity<List<CashMovementDto>> cashMovements(@PathVariable("shiftNumber") String shiftNumber) {
        return ResponseEntity.ok(shiftService.listCashMovements(shiftNumber));
    }
}
