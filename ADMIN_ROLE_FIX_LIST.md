# ADMIN 권한 수정이 필요한 컨트롤러 목록

## 수정이 필요한 파일들

### 1. ReportController.java
**파일 위치**: `backend/src/main/java/com/example/pproject/report/controller/ReportController.java`

**수정 필요한 부분들**:
```java
// 현재 (잘못됨)
@PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER', 'ADMIN', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")

// 수정 후 (올바름)
@PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
```

**수정할 메서드들**:
- `getReport()` - 라인 80
- `getReportsByReporter()` - 라인 107
- `getReportsByStatus()` - 라인 136
- `getReportsByTargetType()` - 라인 152
- `processReport()` - 라인 168
- `getReportCountByTargetMember()` - 라인 188
- `getReportCountByTargetJob()` - 라인 206
- `getMemberPenaltyPoints()` - 라인 224
- `getMemberPenaltyHistory()` - 라인 245

### 2. AdminWalletController.java
**파일 위치**: `backend/src/main/java/com/example/pproject/wallet/controller/AdminWalletController.java`

**수정 필요한 부분**:
```java
// 현재 (잘못됨)
@PreAuthorize("hasRole('ADMIN')")

// 수정 후 (올바름)
@PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
```

### 3. AdminMemberController.java
**파일 위치**: `backend/src/main/java/com/example/pproject/user/controller/AdminMemberController.java`

**수정 필요한 부분**:
```java
// 현재 (잘못됨)
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")

// 수정 후 (올바름)
@PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
```

### 4. AdminProductController.java
**파일 위치**: `backend/src/main/java/com/example/pproject/product/controller/AdminProductController.java`

**수정 필요한 부분**:
```java
// 현재 (잘못됨)
@PreAuthorize("hasRole('ADMIN')")

// 수정 후 (올바름)
@PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
```

### 5. AdminPaymentController.java
**파일 위치**: `backend/src/main/java/com/example/pproject/payment/controller/AdminPaymentController.java`

**수정 필요한 부분**:
```java
// 현재 (잘못됨)
@PreAuthorize("hasRole('ADMIN')")

// 수정 후 (올바름)
@PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
```

### 6. AdminJobController.java
**파일 위치**: `backend/src/main/java/com/example/pproject/job/controller/AdminJobController.java`

**수정 필요한 부분**:
```java
// 현재 (잘못됨)
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")

// 수정 후 (올바름)
@PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
```

### 7. AdminEmployerController.java
**파일 위치**: `backend/src/main/java/com/example/pproject/employer/controller/AdminEmployerController.java`

**수정 필요한 부분**:
```java
// 현재 (잘못됨)
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")

// 수정 후 (올바름)
@PreAuthorize("hasAnyRole('SERVICEADMIN', 'APPROVEADMIN', 'MASTER')")
```

## 수정 방법

각 파일에서 `ADMIN`을 제거하고 올바른 권한으로 변경하면 됩니다.

### 권한 레벨 가이드라인
- **일반 관리 기능**: `SERVICEADMIN`, `APPROVEADMIN`, `MASTER`
- **중요한 관리 기능** (감사로그 등): `APPROVEADMIN`, `MASTER`
- **최고 권한 필요**: `MASTER`

## 올바른 Role 타입
- CANDIDATE
- EMPLOYER  
- SERVICEADMIN
- APPROVEADMIN
- MASTER

**ADMIN은 존재하지 않는 권한입니다!**