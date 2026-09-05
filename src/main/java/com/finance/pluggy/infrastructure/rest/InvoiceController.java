package com.finance.pluggy.infrastructure.rest;

import com.finance.pluggy.domain.service.InvoiceService;
import com.finance.pluggy.infrastructure.rest.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Retorna a lista de faturas ativas e projetadas para todos os cartões de crédito.
     */
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getInvoices() {
        return ResponseEntity.ok(invoiceService.getInvoices());
    }
}
