package com.example.pproject.report.repository;

import com.example.pproject.report.entity.MemberPenaltyPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemberPenaltyPointRepository extends JpaRepository<MemberPenaltyPoint, Long> {

    /**
     * 회원 ID로 경고 점수 조회
     */
    List<MemberPenaltyPoint> findByMemberId(Long memberId);

    /**
     * 회원 ID로 총 경고 점수 조회
     */
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM MemberPenaltyPoint p WHERE p.memberId = :memberId")
    Integer getTotalPenaltyPoints(@Param("memberId") Long memberId);
}