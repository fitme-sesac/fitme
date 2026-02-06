package com.example.pproject.employer.repository;

import com.example.pproject.employer.entity.EmployerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployerRepository extends JpaRepository<EmployerEntity, Long> {
    
    Optional<EmployerEntity> findByEmployerUid(UUID employerUid);
    
    Optional<EmployerEntity> findByIdAndDeletedAtIsNull(Long id);
    
    boolean existsByName(String name);

    // 활성 상태 기업 목록 (삭제되지 않은 것)
    @Query("SELECT e FROM EmployerEntity e WHERE e.status = 'ACTIVE' AND e.deletedAt IS NULL ORDER BY e.createdAt DESC")
    Page<EmployerEntity> findActiveEmployers(Pageable pageable);

    // 업종별 기업 목록
    @Query("SELECT e FROM EmployerEntity e WHERE e.status = 'ACTIVE' AND e.deletedAt IS NULL AND LOWER(e.industry) LIKE LOWER(CONCAT('%', :industry, '%')) ORDER BY e.createdAt DESC")
    Page<EmployerEntity> findByIndustry(@Param("industry") String industry, Pageable pageable);

    // 공개 기업 상세 조회
    @Query("SELECT e FROM EmployerEntity e WHERE e.id = :id AND e.status = 'ACTIVE' AND e.deletedAt IS NULL")
    Optional<EmployerEntity> findPublicById(@Param("id") Long id);

    // 관리자 기업 검색 (기업명)
    Page<EmployerEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // 관리자 기업 목록: 상태별 필터 (ACTIVE=승인, REJECTED=거절)
    Page<EmployerEntity> findByStatus(String status, Pageable pageable);

    Page<EmployerEntity> findByStatusAndNameContainingIgnoreCase(String status, String name, Pageable pageable);
}
