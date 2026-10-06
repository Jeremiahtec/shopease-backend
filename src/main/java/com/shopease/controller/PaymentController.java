package com.shopease.controller;

import com.shopease.dto.payment.InitializePaymentRequest;
import com.shopease.dto.payment.PaymentInitResponse;
import com.shopease.dto.payment.PaymentResponse;
import com.shopease.exception.BadRequestException;
import com.shopease.security.AppUserDetails;
import com.shopease.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "8. Payments (Paystack)")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Customer: start payment for a PENDING order",
            description = "Open the returned authorizationUrl in a browser. In mock mode (no Paystack key) it completes instantly.")
    public PaymentInitResponse initialize(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                          @Valid @RequestBody InitializePaymentRequest request) {
        return paymentService.initialize(principal.getId(), request.orderId());
    }

    @GetMapping("/verify/{reference}")
    @Operation(summary = "Verify a payment by reference (marks the order PAID when successful)")
    public PaymentResponse verify(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                  @PathVariable String reference) {
        return paymentService.verifyForUser(principal, reference);
    }

    @GetMapping("/callback")
    @Operation(summary = "Paystack redirect target after checkout (public)")
    public PaymentResponse callback(@RequestParam(required = false) String reference,
                                    @RequestParam(required = false) String trxref) {
        String ref = reference != null && !reference.isBlank() ? reference : trxref;
        if (ref == null || ref.isBlank()) {
            throw new BadRequestException("Missing payment reference");
        }
        return paymentService.verifyByCallback(ref);
    }

    @PostMapping("/webhook")
    @Operation(summary = "Paystack webhook (signature-checked, called by Paystack only)")
    public ResponseEntity<Void> webhook(@RequestHeader(value = "x-paystack-signature", required = false) String signature,
                                        @RequestBody String payload) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
