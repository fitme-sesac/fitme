package com.example.pproject.user.repository;

import com.example.pproject.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUserid(String userid);
    Optional<UserEntity> findByEmail(String email);

// 비밀번호 찾기
    Optional<UserEntity> findByUseridAndUsernameAndBirthday(String userid, String username, String birthday);

    // 비밀번호 찾기(링크 발송): 아이디+이름+이메일로 검증
    Optional<UserEntity> findByUseridAndUsernameAndEmail(String userid, String username, String email);

    // 인증된 휴대폰은 1계정에만 귀속(논리삭제 제외)
    Optional<UserEntity> findFirstByPhoneAndPhoneVerifiedAtIsNotNullAndDeletedAtIsNull(String phone);

    // 아이디 찾기(휴대폰): 이름 + 휴대폰 번호로 사용자 조회(논리삭제 제외)
    Optional<UserEntity> findFirstByUsernameAndPhoneAndDeletedAtIsNull(String username, String phone);

    UserEntity findByEmailAndBirthdayAndUsername(String email, String birthday, String username);

    // 특정 시점 이후 가입한 회원 수 조회 (삭제되지 않은 회원만)
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :since AND u.deletedAt IS NULL")
    long countByCreatedAtAfterAndDeletedAtIsNull(@Param("since") Instant since);

    // 주간 신규 회원 수 조회
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :weekAgo AND u.deletedAt IS NULL")
    long countWeeklyNewMembers(@Param("weekAgo") Instant weekAgo);
}
