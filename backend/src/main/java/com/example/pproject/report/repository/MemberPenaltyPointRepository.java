package com.example.pproject.report.repository;

import com.example.pproject.report.entity.MemberPenaltyPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemberPenaltyPointRepository extends JpaRepository<MemberPenaltyPoint, Long> {

    List<MemberPenaltyPoint> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 회원의 총 벌점 합계 계산 (JPQL)
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM MemberPenaltyPoint p WHERE p.memberId = :memberId")
    Integer sumPointsByMemberId(@Param("memberId") Long memberId);
}