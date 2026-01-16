from app.resumes.schemas import ResumeRequest
import json
from pydantic import ValidationError

# 이 파일은 'ResumeRequest' 스키마(데이터 구조)가 복잡한 실제 이력서 데이터를 
# 제대로 처리할 수 있는지 확인하는 테스트 스크립트입니다.

# 테스트용 상세 데이터 (JSON 문자열)
# 실제 프론트엔드에서 넘어올 법한 구체적인 이력서 예시를 담고 있습니다.
test_json = """
{
  "resume_id": 2,
  "basic_info": {
    "title": "기본기를 바탕으로 확장성 있는 코드를 고민하는 신입 백엔드 개발자",
    "tagline": "문제를 끝까지 추적하여 해결하는 집요함을 가진 개발자입니다.",
    "re_stack": ["Java", "Spring Boot", "MySQL", "JPA", "AWS EC2", "JUnit5"],
    "field": "RESUME",
    "preference": {
      "location": "서울 전체",
      "salary": "3,400만 원 이상",
      "employment_type": "FULL_TIME"
    }
  },
  "content": "안녕하세요, 6개월간의 집중적인 부트캠프를 통해 백엔드 개발의 기초를 다진 신입 개발자입니다. 단순히 기능을 구현하는 것에 그치지 않고, '왜 이 기술을 써야 하는가'에 대해 끊임없이 자문하며 학습합니다. 팀 프로젝트 진행 시 발생했던 N+1 쿼리 문제를 해결하기 위해 Fetch Join을 학습하고 적용하여 API 성능을 2배 이상 개선한 경험이 있습니다. 클린 코드와 테스트 코드 작성의 중요성을 인지하고 있으며, 동료들과의 코드 리뷰를 통해 함께 성장하는 과정에서 큰 보람을 느낍니다. 빠르게 변화하는 기술 트렌드 속에서 핵심 원리를 놓치지 않는 개발자가 되겠습니다.",
  "projects": [
    {
      "project_name": "중고 거래 플랫폼 '당근마켓' 클론 프로젝트 (팀)",
      "period": "2025.10 - 2025.12",
      "total_tech_stack": ["Java", "Spring Boot", "MySQL", "Redis", "S3", "GitHub Actions", "Docker"],
      "my_tech_stack": ["Java", "Spring Boot", "MySQL", "JPA"],
      "contribution": "실시간 채팅 알림 서버 및 상품 게시판 CRUD 구현",
      "description": "팀 프로젝트에서 백엔드 파트를 담당했습니다. 전체 팀은 Redis를 활용한 채팅 기능을 구현했으나, 저는 그중 데이터베이스 설계와 상품 관리 API 구현에 집중했습니다. 특히, 대량의 데이터 조회 시 발생하는 페이징 처리 속도를 개선하기 위해 인덱스를 활용했으며, JUnit5를 활용해 서비스 로직의 테스트 커버리지를 80% 이상 확보했습니다."
    },
    {
      "project_name": "개인 프로젝트: 날씨 기반 옷차림 추천 서비스",
      "period": "2025.07 - 2025.08",
      "total_tech_stack": ["Java", "Spring Boot", "MariaDB", "OpenWeatherMap API", "AWS EC2"],
      "my_tech_stack": ["Java", "Spring Boot", "MariaDB", "AWS EC2"],
      "contribution": "공공 API 연동 및 서버 배포 전 과정 수행",
      "description": "외부 API를 연동하여 데이터를 가공하고 사용자에게 제공하는 전체 프로세스를 경험했습니다. AWS EC2와 RDS를 연동하여 실제 서비스 운영 환경을 구축해보았으며, 리눅스 환경에서의 기본적인 서버 명령어와 배포 자동화에 대한 기초 지식을 습득했습니다."
    }
  ],
  "careers": [
    {
      "company_name": "OO 카페",
      "role": "아르바이트 및 매장 관리",
      "period": "2022.03 - 2024.02",
      "description": "2년간 성실하게 근무하며 고객 응대 및 재고 관리 업무를 수행했습니다. 반복되는 재고 기록 업무를 엑셀 시트로 자동화하여 업무 시간을 단축하는 등 효율적인 업무 처리에 관심이 많습니다."
    }
  ]
}
"""

try:
    # JSON 문자열을 파이썬 딕셔너리로 변환
    data = json.loads(test_json)
    
    # ResumeRequest 모델을 사용하여 데이터 유효성 검증(Validation) 수행
    request = ResumeRequest(**data)
    
    print("✓ 스키마 유효성 검사 성공 (Schema validation successful!)")
    print(f"이력서 ID: {request.resume_id}")
    print(f"기본 정보 필드: {request.basic_info.field}")
    print(f"프로젝트 개수: {len(request.projects)}")
    
except ValidationError as e:
    # 필수 필드가 누락되었거나 데이터 타입이 맞지 않을 경우 에러 발생
    print("✗ 스키마 유효성 검사 실패 (Schema validation failed):")
    print(e)
except Exception as e:
    # 그 외 알 수 없는 에러 발생 시
    print(f"✗ 오류 발생 (An error occurred): {e}")
