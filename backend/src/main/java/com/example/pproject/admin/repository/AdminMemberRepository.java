package com.example.pproject.admin.repository;

import com.example.pproject.admin.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminMemberRepository extends JpaRepository<Member, Long>,
        JpaSpecificationExecutor<Member> {

    long countByStatus(String status);
    long countByRole(String role);
}