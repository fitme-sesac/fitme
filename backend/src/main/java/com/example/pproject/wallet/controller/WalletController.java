package com.example.pproject.wallet.controller;

import com.example.pproject.Constant.RoleType;
import com.example.pproject.wallet.dto.*;
import com.example.pproject.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // =================================================================================
    // 1. 사용자 기능 (User)
    // =================================================================================

    /**
     * 내 지갑 정보 조회
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletInfoResponse> getMyWallet(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "CANDIDATE") RoleType roleType
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(WalletInfoResponse.from(walletService.getMyWallet(userId, roleType)));
    }

    /**
     * 내 거래 내역 조회 (페이징)
     */
    @GetMapping("/me/ledgers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WalletLedgerResponse>> getMyLedgers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "CANDIDATE") RoleType roleType,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(walletService.getMyLedgers(userId, roleType, pageable));
    }

    /**
     * 내 거래 내역 월별 조회 (페이징)
     */
    @GetMapping("/me/ledgers/monthly")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WalletLedgerResponse>> getMyLedgersByMonth(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "CANDIDATE") RoleType roleType,
            @RequestParam int year,
            @RequestParam int month,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(walletService.getMyLedgersByMonth(userId, roleType, year, month, pageable));
    }

    /**
     * 내 유효 크레딧 목록 조회
     */
    @GetMapping("/me/credit-lots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WalletCreditLotResponse>> getMyCreditLots(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "CANDIDATE") RoleType roleType
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                walletService.getMyCreditLots(userId, roleType).stream()
                        .map(WalletCreditLotResponse::from)
                        .collect(Collectors.toList())
        );
    }

    /**
     * 크레딧 충전 (결제 완료 후 호출)
     */
    @PostMapping("/charge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> chargeCredit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid WalletChargeRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        // TODO: request에 roleType 추가 필요 (현재는 CANDIDATE 고정)
        RoleType roleType = RoleType.CANDIDATE;

        walletService.chargeCredit(userId, roleType, request.amount(), request.toMoney(), request.paymentId());
        return ResponseEntity.ok().build();
    }

    /**
     * 크레딧 사용 (상품 구매 등)
     */
    @PostMapping("/use")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> useCredit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid WalletUseRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        // TODO: request에 roleType 추가 필요 (현재는 CANDIDATE 고정)
        RoleType roleType = RoleType.CANDIDATE;

        walletService.useCredit(userId, roleType, request.amount(), request.orderId(), request.sourceType());
        return ResponseEntity.ok().build();
    }
}
