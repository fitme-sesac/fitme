package com.example.pproject.Constant;

/**
 * 인재 포지션 제안 상태
 */
public enum ProposalStatus {
    PENDING,      // 대기 중 (구직자가 아직 확인 안함)
    VIEWED,       // 확인함
    ACCEPTED,     // 수락함
    REJECTED,     // 거절함
    EXPIRED,      // 만료됨
    CANCELED      // 기업이 취소함
}
