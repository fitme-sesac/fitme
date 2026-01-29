package com.example.pproject.admin.repository;

import com.example.pproject.admin.dto.request.MemberFilterRequest;
import com.example.pproject.admin.entity.Member;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class MemberSpecification {

    public static Specification<Member> getFilter(MemberFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 기본 정보 검색
            if (StringUtils.hasText(request.getName())) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + request.getName() + "%"));
            }
            if (StringUtils.hasText(request.getLogin_id())) {
                predicates.add(criteriaBuilder.like(root.get("loginId"), "%" + request.getLogin_id() + "%"));
            }
            if (StringUtils.hasText(request.getEmail())) {
                predicates.add(criteriaBuilder.like(root.get("email"), "%" + request.getEmail() + "%"));
            }

            // 2. 상태/권한 검색
            if (StringUtils.hasText(request.getRole())) {
                predicates.add(criteriaBuilder.equal(root.get("role"), request.getRole()));
            }
            if (StringUtils.hasText(request.getStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            // ✅ [수정] 등급(MemberGrade) 검색 기능 복구 (Entity에 필드가 존재함)
            if (StringUtils.hasText(request.getMemberGrade())) {
                predicates.add(criteriaBuilder.equal(root.get("memberGrade"), request.getMemberGrade()));
            }

            // 3. 날짜 검색
            if (StringUtils.hasText(request.getDateRange()) && request.getDateRange().contains("~")) {
                try {
                    String[] dates = request.getDateRange().split("~");
                    LocalDate startDate = LocalDate.parse(dates[0].trim());
                    LocalDate endDate = LocalDate.parse(dates[1].trim());
                    predicates.add(criteriaBuilder.between(root.get("createdAt"),
                            startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)));
                } catch (Exception ignored) {}
            }

            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}