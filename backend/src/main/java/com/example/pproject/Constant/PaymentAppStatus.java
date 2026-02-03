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

    /**
     * 상태 전이 가능 여부를 확인합니다.
     * @param nextStatus 변경하려는 다음 상태
     * @return 전이 가능하면 true
     */
    public boolean canTransitionTo(PaymentAppStatus nextStatus) {
        // 자기 자신으로의 전이는 허용하지 않음 (상태 변경이 아니므로)
        if (this == nextStatus) return false;

        return switch (this) {
            case REQUESTED -> Set.of(APPROVED, FAILED).contains(nextStatus);
            case APPROVED -> Set.of(CANCELED, PARTIAL_CANCELED).contains(nextStatus);
            case PARTIAL_CANCELED -> Set.of(CANCELED, PARTIAL_CANCELED).contains(nextStatus);
            // FAILED, CANCELED는 최종 상태이므로 전이 불가
            case FAILED, CANCELED -> false;
        };
    }
}
