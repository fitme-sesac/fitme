package com.example.pproject.wallet.controller;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.wallet.dto.WalletManualChargeRequest;
import com.example.pproject.wallet.dto.WalletManualDeductRequest;
import com.example.pproject.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // 클래스 레벨에서 관리자 권한 체크
public class AdminWalletController {

    private final WalletService walletService;

    /**
     * [관리자] 수동 지급
     */
    @PostMapping("/{walletId}/manual-charge")
    public ResponseEntity<Void> manualCharge(
            @PathVariable Long walletId,
            @RequestBody @Valid WalletManualChargeRequest request) {
        walletService.manualCharge(walletId, request.amount(), request.memo());
        return ResponseEntity.ok().build();
    }

    /**
     * [관리자] 수동 차감
     */
    @PostMapping("/{walletId}/manual-deduct")
    public ResponseEntity<Void> manualDeduct(
            @PathVariable Long walletId,
            @RequestBody @Valid WalletManualDeductRequest request) {
        walletService.manualDeduct(walletId, request.amount(), request.memo());
        return ResponseEntity.ok().build();
    }

    /**
     * [관리자] 지갑 수동 생성
     */
    @PostMapping("/create")
    public ResponseEntity<Long> createWallet(
            @RequestParam Long userId,
            @RequestParam BuyerType buyerType) {
        Long walletId = walletService.createWallet(userId, buyerType);
        return ResponseEntity.ok(walletId);
    }

    /**
     * [관리자] 지갑 상태 변경 (정지/재개)
     */
    @PostMapping("/{walletId}/status")
    public ResponseEntity<Void> changeWalletStatus(
            @PathVariable Long walletId,
            @RequestParam boolean suspend) {
        walletService.changeWalletStatus(walletId, suspend);
        return ResponseEntity.ok().build();
    }
}
