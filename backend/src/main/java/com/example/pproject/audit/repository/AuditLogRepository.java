package com.example.pproject.audit.repository;

import com.example.pproject.audit.dto.AuditLogFilterRequest;
import com.example.pproject.audit.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    // 검색 필터 Specification 정의
    static Specification<AuditLog> getFilter(AuditLogFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getActor_member_id() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorMemberId"), request.getActor_member_id()));
            }
            if (StringUtils.hasText(request.getTarget_type())) {
                predicates.add(criteriaBuilder.equal(root.get("targetType"), request.getTarget_type()));
            }
            if (StringUtils.hasText(request.getAction())) {
                predicates.add(criteriaBuilder.equal(root.get("action"), request.getAction()));
            }
            if (StringUtils.hasText(request.getStartDate()) && StringUtils.hasText(request.getEndDate())) {
                try {
                    predicates.add(criteriaBuilder.between(root.get("createdAt"),
                            LocalDate.parse(request.getStartDate()).atStartOfDay(),
                            LocalDate.parse(request.getEndDate()).atTime(LocalTime.MAX)));
                } catch (Exception ignored) {}
            }

            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}