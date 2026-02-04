package com.example.pproject.Constant;

import java.util.Set;

public enum PaymentAppStatus {
    REQUESTED("결제 요청"),
    APPROVED("승인 완료"),
    FAILED("승인/요청 실패"),
    CANCELED("전체 취소"),
    PARTIAL_CANCELED("부분 취소");

    private final String description;

    PaymentAppStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    private static final Set<PaymentAppStatus> REQUESTED_ALLOWED =
            Set.of(APPROVED, FAILED, CANCELED);
    private static final Set<PaymentAppStatus> APPROVED_ALLOWED =
            Set.of(CANCELED, PARTIAL_CANCELED);

    /**
     * 상태 전이 가능 여부를 확인합니다.
     *
     * @param target 목표 상태
     * @return 전이 가능하면 true
     */
    public boolean canTransitionTo(PaymentAppStatus target) {
        return switch (this) {
            case REQUESTED -> REQUESTED_ALLOWED.contains(target);
            case APPROVED -> APPROVED_ALLOWED.contains(target);
            case FAILED, CANCELED, PARTIAL_CANCELED -> false;
        };
    }
}