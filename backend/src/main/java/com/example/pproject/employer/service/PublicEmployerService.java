package com.example.pproject.employer.service;

import com.example.pproject.employer.dto.PublicEmployerDTO;
import com.example.pproject.employer.dto.PublicEmployerListDTO;
import com.example.pproject.employer.entity.EmployerEntity;
import com.example.pproject.employer.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공개 기업 정보 서비스
 * - 채용 공고가 있는 기업 목록 조회
 * - 기업 상세 정보 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicEmployerService {

    private final EmployerRepository employerRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 공개 기업 목록 조회
     * - OPEN 상태의 채용공고가 있는 기업만 반환
     * - 채용 중인 공고 수와 함께 반환
     */
    public PublicEmployerListDTO getPublicEmployers(int page, int size, String industry) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT 
                    e.employer_id,
                    e.name,
                    e.logo_url,
                    e.industry,
                    e.location,
                    e.employee_count,
                    e.description,
                    COUNT(jp.job_id) as open_job_count
                FROM employer e
                JOIN job_posting jp ON jp.employer_id = e.employer_id 
                    AND jp.status = 'OPEN' 
                    AND jp.deleted_at IS NULL
                WHERE e.deleted_at IS NULL 
                    AND e.status = 'ACTIVE'
                """);

            if (industry != null && !industry.isBlank()) {
                sql.append(" AND e.industry = ?");
            }

            sql.append("""
                GROUP BY e.employer_id, e.name, e.logo_url, e.industry, e.location, e.employee_count, e.description
                HAVING COUNT(jp.job_id) > 0
                ORDER BY open_job_count DESC, e.name ASC
                LIMIT ? OFFSET ?
                """);

            int offset = page * size;
            List<PublicEmployerDTO> employers;

            if (industry != null && !industry.isBlank()) {
                employers = jdbcTemplate.query(sql.toString(),
                        (rs, rowNum) -> PublicEmployerDTO.builder()
                                .employerId(rs.getLong("employer_id"))
                                .name(rs.getString("name"))
                                .logoUrl(rs.getString("logo_url"))
                                .industry(rs.getString("industry"))
                                .location(rs.getString("location"))
                                .employeeCount(rs.getInt("employee_count"))
                                .description(rs.getString("description"))
                                .openJobCount(rs.getInt("open_job_count"))
                                .build(),
                        industry, size, offset);
            } else {
                employers = jdbcTemplate.query(sql.toString(),
                        (rs, rowNum) -> PublicEmployerDTO.builder()
                                .employerId(rs.getLong("employer_id"))
                                .name(rs.getString("name"))
                                .logoUrl(rs.getString("logo_url"))
                                .industry(rs.getString("industry"))
                                .location(rs.getString("location"))
                                .employeeCount(rs.getInt("employee_count"))
                                .description(rs.getString("description"))
                                .openJobCount(rs.getInt("open_job_count"))
                                .build(),
                        size, offset);
            }

            // 전체 개수 조회
            String countSql = """
                SELECT COUNT(DISTINCT e.employer_id)
                FROM employer e
                JOIN job_posting jp ON jp.employer_id = e.employer_id 
                    AND jp.status = 'OPEN' 
                    AND jp.deleted_at IS NULL
                WHERE e.deleted_at IS NULL 
                    AND e.status = 'ACTIVE'
                """;
            
            Long totalElements = jdbcTemplate.queryForObject(countSql, Long.class);
            int totalPages = (int) Math.ceil((double) totalElements / size);

            return PublicEmployerListDTO.builder()
                    .employers(employers)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements != null ? totalElements : 0)
                    .totalPages(totalPages)
                    .build();
        } catch (Exception e) {
            log.warn("공개 기업 목록 조회 실패: {}", e.getMessage());
            return PublicEmployerListDTO.builder()
                    .employers(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }
    }

    /**
     * 공개 기업 상세 조회
     */
    public PublicEmployerDTO getPublicEmployer(Long employerId) {
        EmployerEntity employer = employerRepository.findByIdAndDeletedAtIsNull(employerId)
                .orElseThrow(() -> new IllegalStateException("기업을 찾을 수 없습니다."));

        if (!"ACTIVE".equals(employer.getStatus())) {
            throw new IllegalStateException("기업을 찾을 수 없습니다.");
        }

        // 채용 공고 수 조회
        String countSql = """
            SELECT COUNT(*) FROM job_posting 
            WHERE employer_id = ? AND status = 'OPEN' AND deleted_at IS NULL
            """;
        Integer openJobCount = jdbcTemplate.queryForObject(countSql, Integer.class, employerId);

        return PublicEmployerDTO.builder()
                .employerId(employer.getId())
                .name(employer.getName())
                .logoUrl(employer.getLogoUrl())
                .industry(employer.getIndustry())
                .location(employer.getLocation())
                .employeeCount(employer.getEmployeeCount())
                .description(employer.getDescription())
                .openJobCount(openJobCount != null ? openJobCount : 0)
                .build();
    }
}
