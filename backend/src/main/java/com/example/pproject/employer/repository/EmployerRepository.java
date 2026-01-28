package com.example.pproject.employer.repository;

import com.example.pproject.employer.entity.EmployerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployerRepository extends JpaRepository<EmployerEntity, Long> {
    
    Optional<EmployerEntity> findByEmployerUid(UUID employerUid);
    
    Optional<EmployerEntity> findByIdAndDeletedAtIsNull(Long id);
    
    boolean existsByName(String name);
}
