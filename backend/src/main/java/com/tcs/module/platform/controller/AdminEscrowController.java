package com.tcs.module.platform.controller;

import com.tcs.module.finance.dto.response.AdminEscrowPageResponse;
import com.tcs.module.finance.dto.response.AdminEscrowResponse;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.service.AdminEscrowService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/platform/escrows") @RequiredArgsConstructor
public class AdminEscrowController {
    private final AdminEscrowService service;
    @GetMapping
    public AdminEscrowPageResponse search(@RequestParam(required = false) EscrowStatus status,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String reference, @RequestParam(required = false) String payer,
            @RequestParam(required = false) String beneficiary, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        return service.search(status, from, to, reference, payer, beneficiary, page, size);
    }
    @GetMapping("/{escrowId}") public AdminEscrowResponse get(@PathVariable Long escrowId) { return service.get(escrowId); }
}
