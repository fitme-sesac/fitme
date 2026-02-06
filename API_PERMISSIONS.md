# API 엔드포인트별 권한 설정

## 1. ADMIN 관련 API

### 관리자 대시보드
```
("/admin/dashboard") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/admin/notifications") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
```

## 2. AUDIT (감사 로그) API

### 감사 로그 조회 - 승인 관리자 이상만 접근
```
("/api/v1/admin/audit-logs") = ("APPROVEADMIN", "MASTER");
("/api/v1/admin/audit-logs/**") = ("APPROVEADMIN", "MASTER");
```

## 3. FAQ API

### 공개 FAQ (누구나 접근 가능)
```
("/api/v1/faqs") = ("permitAll");
("/api/v1/faqs/{faqId}") = ("permitAll");
("/api/v1/faqs/search") = ("permitAll");
("/api/v1/faqs/search-full") = ("permitAll");
("/api/v1/faqs/recent") = ("permitAll");
("/api/v1/faqs/recently-updated") = ("permitAll");
```

### FAQ 관리자 기능
```
("/api/v1/faqs/admin") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/faqs/admin/**") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
```

## 4. NOTICE (공지사항) API

### 공개 공지사항 (누구나 접근 가능)
```
("/api/v1/notices") = ("permitAll");
("/api/v1/notices/{noticeId}") = ("permitAll");
("/api/v1/notices/policy/{type}") = ("permitAll");
("/api/v1/notices/type/{type}") = ("permitAll");
("/api/v1/notices/search") = ("permitAll");
```

### 공지사항 관리자 기능
```
("/api/v1/notices/admin") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/notices/admin/**") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
```

## 5. INQUIRY (문의) API

### 문의 등록 - 일반 회원
```
("/api/v1/inquiries").method(POST) = ("CANDIDATE", "EMPLOYER");
```

### 문의 조회 - 본인 또는 관리자
```
("/api/v1/inquiries/{inquiryId}") = ("CANDIDATE", "EMPLOYER", "SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/inquiries/member/{memberId}") = ("CANDIDATE", "EMPLOYER", "SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/inquiries/member/{memberId}/open-count") = ("CANDIDATE", "EMPLOYER", "SERVICEADMIN", "APPROVEADMIN", "MASTER");
```

### 문의 관리 - 관리자 전용
```
("/api/v1/inquiries/status/{status}") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/inquiries/{inquiryId}/reply") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/inquiries/{inquiryId}/status") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
```

### 문의 삭제 - 본인 또는 관리자
```
("/api/v1/inquiries/{inquiryId}").method(DELETE) = ("CANDIDATE", "EMPLOYER", "SERVICEADMIN", "APPROVEADMIN", "MASTER");
```

## 6. REPORT (신고) API

### 신고 등록 - 일반 회원
```
("/api/v1/reports").method(POST) = ("CANDIDATE", "EMPLOYER");
```

### 신고 조회 - 본인 또는 관리자
```
("/api/v1/reports/{reportId}") = ("CANDIDATE", "EMPLOYER", "SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/reports/reporter/{reporterMemberId}") = ("CANDIDATE", "EMPLOYER", "SERVICEADMIN", "APPROVEADMIN", "MASTER");
```

### 신고 관리 - 관리자 전용
```
("/api/v1/reports/status/{status}") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/reports/by-target-type/{targetType}") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/reports/{reportId}/process") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/reports/target-member/{targetMemberId}/count") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/reports/target-job/{targetJobId}/count") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/reports/member/{memberId}/penalty-points") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
("/api/v1/reports/member/{memberId}/penalty-history") = ("SERVICEADMIN", "APPROVEADMIN", "MASTER");
```

## 권한 레벨 설명

- **MASTER**: 최고 관리자 (모든 권한)
- **APPROVEADMIN**: 승인 관리자 (감사 로그 포함 대부분 관리 기능)
- **SERVICEADMIN**: 서비스 관리자 (일반 관리 기능)
- **EMPLOYER**: 기업 회원
- **CANDIDATE**: 구직자 회원
- **permitAll**: 모든 사용자 (비회원 포함)

## 특별 권한 설정

### 감사 로그는 높은 권한 필요
- 감사 로그 관련 API는 APPROVEADMIN 이상만 접근 가능

### 본인 확인이 필요한 API
- 문의 조회/삭제: JWT에서 사용자 ID 추출하여 본인 확인
- 신고 조회: 신고자 본인 또는 관리자만 접근 가능

### HTTP 메서드별 권한 분리
- POST /api/v1/inquiries: 일반 회원만
- DELETE /api/v1/inquiries/{id}: 본인 또는 관리자
- POST /api/v1/reports: 일반 회원만