package com.example.pproject.employer.repository;

import com.example.pproject.employer.entity.EmployerMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerMemberRepository extends JpaRepository<EmployerMemberEntity, Long> {

    // 회원이 소속된 기업 목록 (활성 상태만)
    List<EmployerMemberEntity> findByMemberIdAndActiveTrue(Long memberId);

    // 회원이 소속된 기업 (단일 - 첫 번째 것)
    Optional<EmployerMemberEntity> findFirstByMemberIdAndActiveTrue(Long memberId);

    // 특정 기업-회원 매핑 조회
    Optional<EmployerMemberEntity> findByEmployerIdAndMemberId(Long employerId, Long memberId);

    // 기업의 소속 회원 목록
    List<EmployerMemberEntity> findByEmployerIdAndActiveTrue(Long employerId);

    // 회원이 해당 기업에 소속되어 있는지 확인
    boolean existsByEmployerIdAndMemberIdAndActiveTrue(Long employerId, Long memberId);

    // 회원이 소속된 기업이 있는지 확인
    boolean existsByMemberIdAndActiveTrue(Long memberId);

    // 특정 기업의 특정 직책 멤버 조회 (예: OWNER)
    Optional<EmployerMemberEntity> findFirstByEmployerIdAndRoleInCompanyAndActiveTrue(Long employerId,
            String roleInCompany);
}
