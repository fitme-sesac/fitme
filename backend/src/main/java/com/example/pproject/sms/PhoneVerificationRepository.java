package com.example.pproject.sms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerificationEntity, UUID> {

    Optional<PhoneVerificationEntity>
    findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(String phone, String purpose);
}
