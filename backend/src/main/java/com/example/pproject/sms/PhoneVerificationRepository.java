package com.example.pproject.sms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerificationEntity, UUID> {

    /**
     * Finds the most recently created, unconsumed phone verification for the given phone number and purpose.
     *
     * @param phone   the phone number to match
     * @param purpose the verification purpose to match (e.g., "signup", "reset_password")
     * @return        an Optional containing the latest unconsumed PhoneVerificationEntity for the specified phone and purpose, or empty if none exists
     */
    Optional<PhoneVerificationEntity>
    findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(String phone, String purpose);
}