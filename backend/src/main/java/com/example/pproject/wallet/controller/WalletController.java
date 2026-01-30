package com.example.pproject.wallet.controller;

import com.example.pproject.Constant.BuyerType;
import com.example.pproject.Constant.RoleType;
import com.example.pproject.payment.entity.Payment;
import com.example.pproject.payment.repository.PaymentRepository;
import com.example.pproject.wallet.dto.*;
import com.example.pproject.wallet.service.WalletService;
import com.example.pproject.common.service.DistributedLockService;
import com.example.pproject.global.exception.DuplicateRequestException;
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
        private final PaymentRepository paymentRepository;
        private final DistributedLockService lockService;

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
                        @RequestParam(defaultValue = "CANDIDATE") RoleType roleType) {
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
                        @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
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
                        @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
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
                        @RequestParam(defaultValue = "CANDIDATE") RoleType roleType) {
                Long userId = Long.parseLong(userDetails.getUsername());
                return ResponseEntity.ok(
                                walletService.getMyCreditLots(userId, roleType).stream()
                                                .map(WalletCreditLotResponse::from)
                                                .collect(Collectors.toList()));
        }

        /**
         * 크레딧 충전 (결제 완료 후 호출)
         */
        @PostMapping("/charge")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> chargeCredit(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestBody @Valid WalletChargeRequest request) {
                Long userId = Long.parseLong(userDetails.getUsername());
                // TODO: request에 roleType 추가 필요 (현재는 CANDIDATE 고정)
                BuyerType buyerType = BuyerType.MEMBER;

                Payment payment = paymentRepository.findById(request.paymentId())
                                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

                walletService.chargeCredit(userId, buyerType, request.amount(), request.toMoney(), payment);
                return ResponseEntity.ok().build();
        }

        /**
         * 크레딧 사용 (상품 구매 등)
         */
        @PostMapping("/use")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<Void> useCredit(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestBody @Valid WalletUseRequest request) {
                Long userId = Long.parseLong(userDetails.getUsername());

                // 분산 락 키 생성 (중복 요청 방지)
                String lockKey = "wallet:use:" + userId + ":" + request.orderId();
                if (!lockService.tryLock(lockKey)) {
                        throw new DuplicateRequestException("이미 처리 중인 요청입니다.");
                }

                try {
                        walletService.useCredit(userId, request.buyerType(), request.amount(),
                                        request.orderId(), request.sourceType());
                        return ResponseEntity.ok().build();
                } finally {
                        lockService.unlock(lockKey);
                }
        }
}
