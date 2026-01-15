"""
이 파일은 실제 DB에 저장된 '줄글(Text)' 형태의 이력서를 테스트하기 위한 스크립트입니다.
기존 JSON 구조가 아닌, 단순 텍스트도 AI가 잘 요약하는지 검증합니다.
- 텍스트 형식으로 받는 버전의 테스트

실행 방법:
> python tests/resumes/test_api_text.py
"""
import requests
import json

url = "http://localhost:8000/resumes/process"

# 실제 DB에 저장될 법한 줄글 형태의 이력서 데이터
raw_text_data = """
[자기소개: 비즈니스의 속도를 지탱하는 견고한 아키텍처]

안녕하세요, 시스템의 안정성과 확장성을 최우선 가치로 두는 백엔드 개발자 김서버입니다. 저는 단순히 기능을 구현하는 것을 넘어, "이 코드가 1년 뒤에도 유지보수 가능한가?", "트래픽이 10배로 늘어나도 버틸 수 있는가?"를 끊임없이 고민합니다. 지난 5년 동안 커머스와 핀테크 도메인을 거치며 모놀리식 아키텍처를 마이크로서비스(MSA)로 전환하는 대규모 프로젝트를 주도했습니다.

[핵심 역량]
- Java 17, Spring Boot 3.x, Kotlin 기반의 견고한 서버 개발
- Redis, Kafka를 활용한 대용량 트래픽 처리 및 이벤트 기반 아키텍처 설계
- AWS, Docker, Kubernetes 기반의 인프라 구축 및 CI/CD 파이프라인 자동화

[상세 프로젝트 경험 1: 대규모 선착순 예매 시스템 트래픽 제어]
- 배경: 아이돌 콘서트 티켓 오픈 시 초당 10만 건 이상의 트래픽이 몰려 DB Connection Pool이 고갈되고, 결국 전체 서비스가 다운되는 장애가 발생했습니다.
- 해결 과정: DB 앞단에 트래픽을 제어할 수 있는 "대기열 시스템"을 도입했습니다. 속도가 빠른 In-memory DB인 Redis의 Sorted Set 자료구조를 활용하여, 사용자에게 대기 순번 토큰을 발급하고 유효한 토큰을 가진 사용자만 입장시키는 NetFunnel 알고리즘을 구현했습니다.
- 성과: 서버 증설 없이 동시 접속자 수용량을 기존 2만 명에서 15만 명으로 7.5배 증대시켰으며, 티켓팅 피크 시간대에도 DB CPU 사용률을 40% 미만으로 안정적으로 유지했습니다.

[상세 프로젝트 경험 2: 결제 정산 시스템 MSA 전환]
- 배경: 주문, 결제, 정산 로직이 하나의 거대한 코드베이스에 섞여 있어, 정산 모듈을 수정하면 결제 기능에 사이드 이펙트가 발생하는 등 유지보수가 어려웠습니다.
- 해결 과정: 도메인 주도 설계(DDD)를 기반으로 서비스를 분리했습니다. 서비스 간 통신 과정에서 발생할 수 있는 데이터 정합성 문제를 해결하기 위해 Kafka를 도입하여 이벤트 기반 아키텍처(EDA)를 구축했습니다.
- 성과: 배포 시간을 기존 1시간에서 10분으로 단축하여 개발 생산성을 높였으며, 장애 전파 차단(Circuit Breaker) 패턴을 적용하여 결제 장애가 정산 시스템에 영향을 주지 않도록 격리했습니다.
"""

# API 요청 데이터 구성 (data 필드에 문자열을 그대로 넣음)
data = {
    "id": 2,
    "type": "RESUME",
    "data": raw_text_data 
}

try:
    print(f"Sending RAW TEXT request to {url}...")
    response = requests.post(url, json=data)
    
    print(f"Status Code: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print("\n=== Success! AI Summary Result ===")
        print(result["summary"])
        print("==================================")
    else:
        print("Error response:")
        print(response.text)
except Exception as e:
    print(f"Connection failed: {e}")
