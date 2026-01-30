package com.example.pproject.resume.repository;

import com.example.pproject.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // [표준] 특정 유저의 이력서 목록 조회 (User 엔티티의 ID 참조)
    List<Resume> findAllByUser_Id(Long userId);

    // [표준] 특정 유저의 대표 이력서 조회
    Optional<Resume> findByUser_IdAndPrimaryTrue(Long userId);

    // [표준] 특정 유저의 대표 이력서 조회 (첫 번째만 - 중복 방어용)
    Optional<Resume> findFirstByUser_IdAndPrimaryTrueOrderByLastModifiedAtDesc(Long userId);

    // [표준] 특정 유저의 최근 수정된 이력서 조회 (fallback용)
    Optional<Resume> findFirstByUser_IdOrderByLastModifiedAtDesc(Long userId);

    // [추가] 특정 유저의 이력서 개수 조회 (마이페이지 통계용)
    long countByUser_Id(Long userId);

    // [성능 최적화] 광고 매칭용 임베딩만 조회 (불필요한 조인 방지)
    @Query(value = "SELECT CAST(embedding AS text) FROM resume WHERE member_id = :userId AND is_primary = true", nativeQuery = true)
    Optional<String> findEmbeddingByUserId(@Param("userId") Long userId);

    // 기존 findByUserIdAndPrimaryTrue 호출 시 -> 표준 메소드로 연결
    default Optional<Resume> findByUserIdAndPrimaryTrue(Long userId) {
        return findByUser_IdAndPrimaryTrue(userId);
    }

    // Integer가 들어오더라도 Long으로 변환하여 처리
    default Optional<Resume> findByUserIdAndPrimaryTrue(Integer userId) {
        return findByUser_IdAndPrimaryTrue(Long.valueOf(userId));
    }

    /**
     * [광고용] 대표 이력서 조회 with Fallback
     * 1. is_primary = true인 것 조회 (첫 번째만)
     * 2. 없으면 최근 수정된 이력서로 fallback
     */
    default Optional<Resume> findPrimaryOrLatest(Long userId) {
        return findFirstByUser_IdAndPrimaryTrueOrderByLastModifiedAtDesc(userId)
                .or(() -> findFirstByUser_IdOrderByLastModifiedAtDesc(userId));
    }

    // findAllByUserId 호출 시 -> 표준 메소드로 연결
    default List<Resume> findAllByUserId(Long userId) {
        return findAllByUser_Id(userId);
    }

    // countByUserId 호출 시 -> 표준 메소드로 연결
    default long countByUserId(Long userId) {
        return countByUser_Id(userId);
    }
}