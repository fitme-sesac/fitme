import asyncio
import sys
import os
import io
import json
import logging
import uuid
from datetime import datetime

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

# 프로젝트 루트 경로 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.database import get_db_connection, init_db_extensions
from app.resumes.services.summary import summary_service
from app.resumes.services.vector import vector_service
from app.resumes.repository import resume_repo
from app.resumes.schemas import SummaryType

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("SetupDummyResume")

def setup_dummy_resume_data():
    """
    더미 이력서 데이터를 DB에 Insert하고, 임베딩을 생성합니다.
    """
    conn = get_db_connection()
    conn.autocommit = True
    
    # [Refactor] Loop inside to create unique Member for EACH resume
    # 2. Insert Resume (Batch Processing)
    # 여러 개의 이력서 데이터를 리스트로 정의
    mock_requests = [
        # ------------------------------------------------------------------
        # Data 1: [User Original] 신입 백엔드 (Java/Spring) - 박지영
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "기본기를 바탕으로 확장성 있는 코드를 고민하는 신입 백엔드 개발자",
                "re_stack": ["Java", "Spring Boot", "MySQL", "JPA", "langchain", "llm"],
                "field": "RESUME",
                "preference": {
                    "location": "서울 전체",
                    "salary": "3,400만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "안녕하세요. 새로운 기술과 문제 해결에 도전하는 것을 두려워하지 않는 개발자, 박지영입니다. 백엔드 개발 역량 강화를 위해 심화 부트캠프에 자발적으로 참여했으며 Java Spring Boot 기반 팀 프로젝트에서 팀원들과 함께 분산 아키텍처를 설계하고 구현하여 데이터 유실 방지 및 실시간 백업 시스템을 구축했습니다. 그 결과 최우수상을 수상하며 뛰어난 협업과 기술적 문제 해결 능력을 인정받았습니다. 개인적으로는 AI를 효율적으로 서비스에 접목하고 개인화된 서비스 경험을 제공하고자 유튜브 시청 기록을 분석해 사용자 성향을 분류하는 웹 서비스를 직접 기획하고 개발했습니다. 현재 이 프로젝트는 더 나은 사용자 경험을 위해 리팩토링을 진행하고 있으며, 교내 SW 공모전에서 우수상을 수상하였습니다.다양한 프로젝트 경험을 통해 습득한 기술과 데이터 기반의 문제 해결 능력을 바탕으로, AI 역량을 갖춘 개발자로 성장하여 귀사에 기여하고 싶습니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "유튜브 영상 컨텐츠 추천 서비스 개발",
                    "start_date": "2025.10",
                    "end_date": "2025.12",
                    "total_tech_stack": ["Spring Boot", "MongoDB", "Qdrant(Vector DB)", "OpenAI LLM", "RAG", "Docker"],
                    "contribution": "유튜브 영상의 소비 패턴을 분석하여 개인화된 세부 카테고리 기반 분류를 구현함으로써, 사용자 경험과 콘텐츠 소비의 질을 개선",
                    "description": """[문제] 기존 추천 서비스는 단순 키워드 매칭 기반이라 개인화 부족
[해결]
- 브라우저 시청 기록에서 영상 메타데이터(제목, 태그) 추출 및 MongoDB 저장
- 사용자별 상위 관심 키워드 분석 및 OpenAI LLM 기반 확장 키워드 생성
- Qdrant(Vector DB)에 저장하여 RAG 기반 개인화 추천 구현
[성과]
- 단순 키워드 추천 → 세부 카테고리 기반 개인화 추천으로 고도화
- 사용자 경험 개선
- 교내 SW 공모전 우수상 수상"""
                },
                {
                    "project_name": "코인 거래소 플랫폼 개발(전자화폐 가상자산 거래 서비스)",
                    "start_date": "2025.03",
                    "end_date": "2025.04",
                    "total_tech_stack": ["Java 17", "Spring Boot 3.0", "Spring Cloud (Eureka, Gateway)", "JPA/JPQL", "MySQL 8", "Redis", "Cassandra", "Kafka"],
                    "contribution": """확장성 문제 해결: 고성능 MSA 기반 주문 처리
[문제]
기존 모놀리틱 구조 거래소는 1000명당 TPS 100으로 사용자 증가 시 주문 지연

[해결]
- MSA 아키텍처와 API Gateway를 활용하여 서비스별 라우팅, 인증/인가, 로깅, 공통 기능 중앙화

[성과]
- 사용자 수와 코인 상장 증가에도 원활한 주문 처리, 개발 및 관리 효율성 향상
- 데이터 무결성 보장: 장애 대응 및 주문 안정성 강화

[문제]
서버 중단 시 거래 데이터 유실 위험

[해결]
- 주문 요청을 Kafka 메시지로 전송
- 비동기 잔액 차감 처리, 실패 큐와 DLQ(Dead Letter Queue)로 예외 분리

[성과]
- 수강생이 뽑은 최고의 프로젝트 선정
- 모든 주문 요청 신뢰성 확보
- 장애 발생 시에도 데이터 무결성 유지, 서비스 안정성 강화""",
                    "description": "기존 코인 거래소의 TPS 저하 문제와 데이터 유실 위험을 해결하고, 확장성과 신뢰성을 갖춘 고성능 분산 아키텍처 기반 거래 플랫폼을 구현한 프로젝트, 4인 팀으로 진행"
                }
            ],
            "careers": [
                {
                    "company_name": "크로스포인트",
                    "role": "근로학생",
                    "start_date": "2023.09",
                    "end_date": "2024.02",
                    "description": "업무 초기 어려움을 극복하고 문서화 작업을 통해 후임자 인수인계와 업무 효율 향상... (중략)"
                }
            ],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 2: [User Original] 3년차 프론트엔드 (React/Next.js)
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "사용자 경험을 최우선으로 생각하는 3년차 프론트엔드 개발자",
                "re_stack": ["React", "Next.js", "TypeScript", "TailwindCSS", "Recoil"],
                "field": "RESUME",
                "preference": {
                    "location": "강남구",
                    "salary": "5,000만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "웹 성능 최적화와 디자인 시스템 구축 경험이 풍부한 프론트엔드 개발자입니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "사내 디자인 시스템 구축",
                    "start_date": "2024.01",
                    "end_date": "2024.06",
                    "total_tech_stack": ["React", "Storybook", "Emotion"],
                    "contribution": "공통 컴포넌트 40종 개발 및 문서화",
                    "description": "개발 생산성 50% 향상 기여"
                }
            ],
            "careers": [
                {
                    "company_name": "테크스타트업",
                    "role": "프론트엔드 개발자",
                    "start_date": "2022.03",
                    "end_date": "2025.02",
                    "description": "메인 대시보드 개발 및 유지보수"
                }
            ],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 3: 데이터 분석가 (Data Analyst) - 비즈니스 인사이트 중심
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "데이터로 비즈니스 의사결정을 이끄는 데이터 분석가",
                "re_stack": ["Python", "SQL", "Tableau", "Pandas", "Scikit-learn"],
                "field": "DATA_ANALYST",
                "preference": {
                    "location": "서울 전체",
                    "salary": "4,000만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "데이터 뒤에 숨겨진 '왜(Why)'를 찾아내는 분석가입니다. 마케팅 에이전시에서 근무하며 흩어져 있는 광고 데이터를 통합하고, 고객 생애 가치(LTV) 예측 모델을 개발하여 마케팅 예산 집행 효율을 극대화한 경험이 있습니다. SQL 쿼리 최적화와 시각화 도구 활용에 능숙하며, 비개발 직군과의 원활한 커뮤니케이션을 통해 데이터 기반 문화를 전파합니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "고객 이탈 예측 및 방어 마케팅 모델링",
                    "start_date": "2024.03",
                    "end_date": "2024.05",
                    "total_tech_stack": ["Python", "SQL", "XGBoost", "Tableau"],
                    "contribution": "이탈 위험군 조기 탐지 및 타겟 마케팅을 통한 리텐션 증대",
                    "description": """[문제] 서비스 이용자 이탈률이 증가했으나 원인 파악이 어렵고 사후 대응만 가능한 상태
[해결]
- 유저 행동 로그(접속 빈도, 구매 패턴, CS 문의 등) 기반 피처 엔지니어링 수행
- XGBoost 알고리즘을 활용하여 이탈 확률 예측 모델 개발 (정확도 87%)
- Tableau 대시보드를 구축하여 마케팅 팀이 이탈 위험군을 실시간으로 확인하도록 지원
[성과]
- 이탈 위험 고객 대상 쿠폰 발송 캠페인 진행 결과 리텐션 12% 상승
- 마케팅 비용 대비 효율(ROAS) 20% 개선"""
                }
            ],
            "careers": [
                {
                    "company_name": "데이터인사이트",
                    "role": "데이터 분석 인턴",
                    "start_date": "2023.06",
                    "end_date": "2023.12",
                    "description": "SQL을 활용한 주간 리포트 자동화 및 A/B 테스트 결과 분석 지원"
                }
            ],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 4: 데브옵스 엔지니어 (DevOps) - 인프라 자동화 중심
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "안정적인 서비스 배포와 비용 최적화를 실현하는 DevOps 엔지니어",
                "re_stack": ["AWS", "Kubernetes", "Terraform", "Jenkins", "Ansible", "Prometheus"],
                "field": "DEVOPS",
                "preference": {
                    "location": "서울/재택",
                    "salary": "6,000만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "개발자가 개발에만 집중할 수 있는 환경을 만드는 것을 목표로 합니다. 수동으로 관리되던 온프레미스 인프라를 AWS 클라우드로 이관하고, IaC(Terraform)를 도입하여 인프라 관리의 복잡성을 해결했습니다. CI/CD 파이프라인 고도화를 통해 배포 주기를 단축시키고, 오토스케일링 정책을 최적화하여 클라우드 비용을 절감한 성과가 있습니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "MSA 전환을 위한 Kubernetes 클러스터 구축 및 CI/CD 파이프라인 자동화",
                    "start_date": "2024.07",
                    "end_date": "2024.12",
                    "total_tech_stack": ["AWS EKS", "Terraform", "ArgoCD", "GitHub Actions", "Helm"],
                    "contribution": "배포 프로세스 자동화 및 무중단 배포 환경 구축",
                    "description": """[문제] 모놀리식 구조의 수동 배포 방식으로 인해 배포 시마다 1시간 이상의 다운타임 발생 및 인적 실수 빈발
[해결]
- Terraform을 활용하여 AWS EKS 등 인프라 리소스 코드로 정의(IaC)
- GitHub Actions와 ArgoCD를 연동하여 GitOps 기반의 CD 파이프라인 구축
- Blue/Green 배포 전략을 도입하여 무중단 배포 구현
[성과]
- 배포 소요 시간 1시간 → 5분으로 단축
- 인프라 운영 비용 기존 대비 30% 절감 (Spot Instance 활용)"""
                }
            ],
            "careers": [],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 5: 풀스택 개발자 (Full Stack) - 스타트업/MVP 중심
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "아이디어를 서비스로 빠르게 구현하는 풀스택 개발자",
                "re_stack": ["Node.js", "NestJS", "React", "PostgreSQL", "Docker", "AWS"],
                "field": "FULL_STACK",
                "preference": {
                    "location": "서울 전체",
                    "salary": "4,500만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "비즈니스 요구사항을 빠르게 파악하고 기획부터 배포까지 전 과정을 주도적으로 수행할 수 있습니다. 초기 스타트업에서 1인 개발자로 시작하여 MVP를 2개월 만에 런칭한 경험이 있으며, 프론트엔드와 백엔드 간의 데이터 흐름을 완벽하게 이해하고 설계합니다. 효율적인 코드 재사용과 모듈화를 통해 유지보수가 용이한 시스템을 만듭니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "동네 기반 중고 거래 및 커뮤니티 플랫폼 (MVP)",
                    "start_date": "2024.01",
                    "end_date": "2024.03",
                    "total_tech_stack": ["NestJS", "TypeORM", "React Native", "Socket.io", "Redis"],
                    "contribution": "기획, 디자인, 개발 전 과정 리딩 및 앱 스토어 출시",
                    "description": """[문제] 한정된 예산과 시간 내에 위치 기반 실시간 채팅이 가능한 거래 플랫폼 구축 필요
[해결]
- NestJS와 TypeORM을 사용하여 빠른 백엔드 API 서버 구축
- React Native를 활용하여 iOS/Android 크로스 플랫폼 앱 동시 개발
- Socket.io와 Redis를 활용하여 지연 없는 실시간 1:1 채팅 기능 구현
- Geo-hashing 알고리즘을 적용하여 효율적인 반경 검색 쿼리 구현
[성과]
- 2개월 만에 앱 스토어 심사 통과 및 런칭
- 출시 첫 달 가입자 1,000명 달성, 동시 접속자 100명 환경에서 채팅 지연 0건"""
                }
            ],
            "careers": [],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 6: 프롬프트 엔지니어 (Prompt Engineer) - LLM 활용 중심
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "LLM의 잠재력을 비즈니스 가치로 연결하는 프롬프트 엔지니어",
                "re_stack": ["Python", "OpenAI API", "LangChain", "Prompt Engineering", "Fine-tuning"],
                "field": "AI_ENGINEER",
                "preference": {
                    "location": "재택/원격",
                    "salary": "협의",
                    "employment_type": "CONTRACT"
                }
            },
            "content": "생성형 AI의 답변 품질을 최적화하고 할루시네이션을 제어하는 전문 엔지니어입니다. 다양한 LLM 모델(GPT-4, Claude, Llama 3)의 특성을 이해하고 있으며, RAG(검색 증강 생성) 시스템의 검색 정확도를 높이기 위한 프롬프트 튜닝 경험이 풍부합니다. 정량적 평가 지표를 설계하여 프롬프트 성능을 지속적으로 개선합니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "법률 상담 AI 챗봇 성능 고도화",
                    "start_date": "2024.09",
                    "end_date": "2024.11",
                    "total_tech_stack": ["LangChain", "GPT-4o", "Pinecone", "RAGAS"],
                    "contribution": "답변 정확도 개선 및 할루시네이션(거짓 답변) 최소화",
                    "description": """[문제] 기존 법률 챗봇이 판례를 잘못 인용하거나 없는 법 조항을 생성하는 할루시네이션 문제 심각
[해결]
- Chain-of-Thought (CoT) 기법을 적용하여 모델이 판례 검색 -> 해석 -> 답변 작성 단계를 거치도록 프롬프트 구조화
- RAG 검색 결과가 없을 경우 '모른다'고 답하도록 하는 Negative Constraint 강화
- RAGAS 프레임워크를 활용하여 답변의 신뢰성(Faithfulness) 자동 평가 파이프라인 구축
[성과]
- 답변 정확도 65% → 92% 상승
- 할루시네이션 발생률 20% 미만으로 감소, 변호사 검수 통과율 증가"""
                }
            ],
            "careers": [],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 7: UI/UX 디자이너 (Designer) - 사용자 중심 설계
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "데이터와 논리에 기반하여 디자인하는 Product Designer",
                "re_stack": ["Figma", "ProtoPie", "Zeplin", "User Research", "Design System"],
                "field": "DESIGNER",
                "preference": {
                    "location": "서울 전체",
                    "salary": "4,000만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "단순히 예쁜 디자인이 아닌, 사용자의 문제를 해결하고 비즈니스 지표를 개선하는 디자인을 추구합니다. 정성적/정량적 리서치를 통해 사용자 니즈를 파악하고, 개발자와의 원활한 협업을 위한 디자인 시스템 가이드를 제작한 경험이 있습니다. A/B 테스트를 주도하여 구매 전환율을 개선한 성과가 있습니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "모바일 앱 결제 프로세스 UX 개편",
                    "start_date": "2024.02",
                    "end_date": "2024.05",
                    "total_tech_stack": ["Figma", "User Testing", "Hotjar", "Google Analytics"],
                    "contribution": "결제 단계 간소화를 통한 구매 전환율(CVR) 상승",
                    "description": """[문제] 장바구니에서 결제 완료까지의 이탈률이 60%로 매우 높게 나타남. GA 데이터 분석 결과, 복잡한 주소 입력과 결제 수단 선택 단계에서 이탈 집중.
[해결]
- 사용성 테스트(UT)를 진행하여 입력 폼의 인지 부조화 원인 파악
- 결제 단계를 5단계에서 2단계로 축소하고, 최근 배송지/결제수단 자동 선택 기능 설계
- Figma 프로토타이핑을 통해 개발 구현 가능성 사전 검증
[성과]
- 결제 페이지 이탈률 60% → 35% 감소
- 전체 구매 전환율 15% 상승 달성"""
                }
            ],
            "careers": [],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 8: AI 연구원/엔지니어 (AI Engineer) - 컴퓨터 비전
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "SOTA 모델을 서비스에 경량화하여 적용하는 AI 엔지니어",
                "re_stack": ["PyTorch", "OpenCV", "TensorRT", "Python", "FastAPI"],
                "field": "AI_ENGINEER",
                "preference": {
                    "location": "서울/판교",
                    "salary": "5,500만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "최신 딥러닝 논문을 빠르게 습득하고 실제 프로덕트에 적용하는 능력을 갖췄습니다. 컴퓨터 비전 분야(객체 탐지)에 강점이 있으며, 무거운 모델을 모바일/엣지 디바이스에서도 실시간으로 구동할 수 있도록 경량화(Quantization, Pruning)한 경험이 있습니다. 데이터 파이프라인 구축부터 모델 서빙 API 개발까지 가능합니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "스마트 팩토리 불량품 자동 검출 시스템",
                    "start_date": "2024.04",
                    "end_date": "2024.09",
                    "total_tech_stack": ["PyTorch", "YOLOv8", "TensorRT", "Docker"],
                    "contribution": "검출 정확도 향상 및 추론 속도 최적화",
                    "description": """[문제] 기존 육안 검사 방식의 낮은 정확도와 생산 속도 저하 문제, 기존 AI 모델의 느린 추론 속도
[해결]
- YOLOv8 모델을 커스텀 데이터셋으로 파인튜닝하여 미세한 스크래치 탐지 성능 강화
- Knowledge Distillation 기법을 적용하여 모델 크기 40% 축소
- TensorRT를 활용한 FP16 양자화로 GPU 추론 속도 가속화
[성과]
- 불량 검출 정확도(mAP) 98.5% 달성
- 이미지 장당 처리 속도 0.5초 → 0.05초로 10배 단축, 실시간 라인 적용 성공"""
                }
            ],
            "careers": [],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 9: 정보보안 엔지니어 (Security) - 모의해킹/보안관제
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "선제적 방어로 자산을 보호하는 화이트해커 출신 보안 엔지니어",
                "re_stack": ["Python", "Burp Suite", "Metasploit", "Wireshark", "AWS Security"],
                "field": "SECURITY",
                "preference": {
                    "location": "서울 전체",
                    "salary": "4,500만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "공격자의 관점에서 취약점을 찾고 방어하는 보안 전문가입니다. 웹/앱 모의해킹 수행 경험이 풍부하며, OWASP Top 10 취약점에 대한 이해도가 높습니다. 단순히 취약점을 찾는 것을 넘어, 개발팀과 협업하여 시큐어 코딩 문화를 정착시키고 DevSecOps 파이프라인을 구축하는 데 기여하고 싶습니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "사내 서비스 통합 보안 취약점 점검 및 조치",
                    "start_date": "2024.06",
                    "end_date": "2024.08",
                    "total_tech_stack": ["Burp Suite", "Nessus", "Python Scripting"],
                    "contribution": "치명적 보안 결함 발견 및 조치를 통한 서비스 안정성 확보",
                    "description": """[문제] 신규 런칭 예정인 핀테크 서비스의 보안성 검토 미흡으로 인한 개인정보 유출 우려
[해결]
- 블랙박스 및 그레이박스 테스팅 방식으로 모의해킹 수행
- 인증 우회 및 SQL Injection 취약점 발견 후 PoC(개념 증명) 코드 작성하여 개발팀에 공유
- 시큐어 코딩 가이드라인 제작 및 배포
[성과]
- 총 15건의 취약점(High 등급 3건 포함) 런칭 전 조치 완료
- ISMS-P 인증 심사 요건 충족에 기여"""
                }
            ],
            "careers": [],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        },

        # ------------------------------------------------------------------
        # Data 10: 모바일 앱 개발자 (Mobile - Flutter) - 크로스플랫폼
        # ------------------------------------------------------------------
        {
            "basic_info": {
                "title": "하나의 코드로 완벽한 경험을 만드는 Flutter 개발자",
                "re_stack": ["Flutter", "Dart", "Firebase", "GetX", "Swift"],
                "field": "MOBILE",
                "preference": {
                    "location": "서울/판교",
                    "salary": "4,000만 원 이상",
                    "employment_type": "FULL_TIME"
                }
            },
            "content": "높은 생산성과 네이티브급 성능을 동시에 잡는 크로스 플랫폼 개발을 지향합니다. Flutter로 Android와 iOS 앱을 동시에 배포 및 운영한 경험이 있으며, 상태 관리 패턴(GetX, Provider)을 적절히 활용하여 유지보수하기 좋은 코드를 작성합니다. 복잡한 애니메이션 구현과 네이티브 모듈 연동에도 능숙합니다.",
            "file_links": [],
            "projects": [
                {
                    "project_name": "실시간 배달 라이더 전용 앱 개발",
                    "start_date": "2024.01",
                    "end_date": "2024.05",
                    "total_tech_stack": ["Flutter", "Google Maps API", "Firebase FCM", "WebSocket"],
                    "contribution": "위치 기반 실시간 관제 및 배차 수락 편의성 개선",
                    "description": """[문제] 기존 네이티브 앱의 OS별 기능 파편화로 인한 유지보수 비용 증가 및 업데이트 지연
[해결]
- Flutter 기반으로 앱을 재구축하여 단일 코드베이스 확보
- WebSocket을 활용하여 실시간 주문 알림 및 위치 전송 지연 시간 최소화
- 백그라운드 위치 추적 기능 최적화로 배터리 소모량 감소
[성과]
- Android/iOS 동시 업데이트 배포 프로세스 구축 (배포 기간 3일 → 0.5일 단축)
- 앱 크래시율 99.9% 무사고 달성, 라이더 앱 만족도 4.5점 기록"""
                }
            ],
            "careers": [],
            "summary_type": "STRUCTURED",
            "include_reasoning": False
        }
    ]
    file_list = []  # 테스트용 빈 리스트

    created_pairs = [] # (resume_id, raw_data) tuples
    file_list = []  # 테스트용 빈 리스트

    try:
        with conn.cursor() as cur:
            # 1. Fetch Existing Members
            cur.execute("SELECT member_id FROM member LIMIT 50")
            rows = cur.fetchall()
            if not rows:
                logger.error("❌ No members found in DB. Please create a member first.")
                return []
            
            existing_member_ids = [r[0] for r in rows]
            logger.info(f"✅ Found {len(existing_member_ids)} existing members. Distributing resumes...")

            for idx, mock_request in enumerate(mock_requests):
                # -------------------------------------------------------------
                # 1. Assign Member (Round-Robin)
                # -------------------------------------------------------------
                # 기존 멤버 ID를 순환하며 할당
                member_id = existing_member_ids[idx % len(existing_member_ids)]
                logger.info(f"👤 [Member] Assigning Resume {idx+1} to Member ID: {member_id}")

                # -------------------------------------------------------------
                # 2. Insert Resume
                # -------------------------------------------------------------
                # 2.1 Insert Main Resume
                re_stack_list = mock_request["basic_info"]["re_stack"]
                
                cur.execute("""
                    INSERT INTO resume (
                        member_id, title, is_primary, is_public, status, 
                        content, field, 
                        preference_location, preference_salary, employment_type,
                        re_stack, summary_status,
                        career_years
                    ) VALUES (
                        %s, %s, true, true, 'ACTIVE', 
                        %s, 'RESUME',
                        %s, %s, %s,
                        %s, 'PENDING',
                        %s
                    ) RETURNING resume_id
                """, (
                    member_id,
                    mock_request["basic_info"]["title"],
                    mock_request["content"],
                    mock_request["basic_info"]["preference"]["location"],
                    mock_request["basic_info"]["preference"]["salary"],
                    mock_request["basic_info"]["preference"]["employment_type"],
                    re_stack_list,
                    3 if mock_request["careers"] else 0 # Simple Logic
                ))
                resume_id = cur.fetchone()[0]
                logger.info(f"🛠️ [Insert {idx+1}] Resume created with ID: {resume_id}")
                
                created_pairs.append((resume_id, mock_request))

                # 2.2 Insert Projects
                for p in mock_request["projects"]:
                    s_date = f"{p['start_date'].replace('.', '-')}-01"
                    e_date = f"{p['end_date'].replace('.', '-')}-01" if p['end_date'] else None
                    full_desc = f"[Contribution]\n{p['contribution']}\n\n[Description]\n{p['description']}"

                    cur.execute("""
                        INSERT INTO resume_project (
                            resume_id, title, start_date, end_date, tech_stack, description
                        ) VALUES (
                            %s, %s, %s, %s, %s, %s
                        )
                    """, (
                        resume_id, p["project_name"], s_date, e_date, p["total_tech_stack"], full_desc
                    ))
                
                # 2.3 Insert Careers
                for c in mock_request["careers"]:
                    s_date = f"{c['start_date'].replace('.', '-')}-01"
                    e_date = f"{c['end_date'].replace('.', '-')}-01" if c['end_date'] else None
                    
                    cur.execute("""
                        INSERT INTO resume_career (
                            resume_id, company_name, department, role, start_date, end_date
                        ) VALUES (
                            %s, %s, 'N/A', %s, %s, %s
                        )
                    """, (
                        resume_id, c["company_name"], c["role"], s_date, e_date
                    ))

        conn.close()
        return created_pairs

    except Exception as e:
        logger.error(f"❌ Error inserting dummy resumes: {e}")
        conn.close()
        return []

async def generate_and_save_embedding(resume_id, raw_data):
    """
    저장된 이력서 데이터를 바탕으로 요약을 생성하고 임베딩을 저장합니다.
    """
    if not resume_id:
        return

    logger.info(f"🔄 [AI Processing] Generating summary & embedding for Resume ID: {resume_id}...")
    
    try:
        s_type = SummaryType(raw_data.get("summary_type", "STRUCTURED"))
        
        result_obj, metadata = await summary_service.generate_summary(
            data=raw_data, 
            type="RESUME", 
            summary_type=s_type
        )
        
        # [Modified] Save BOTH Display Summary and Embedding Text in one column
        display_text = result_obj.to_formatted_string(include_reasoning=raw_data["include_reasoning"])
        
        meta_title = raw_data["basic_info"]["title"]
        meta_stack = ", ".join(raw_data["basic_info"]["re_stack"])
        embedding_text = result_obj.to_embedding_string(title=meta_title, tech_stack=meta_stack)
        
        # 구분자로 합체
        summary_text = f"{display_text}\n\n<<<<EMBEDDING_SOURCE>>>>\n\n{embedding_text}"
        
        # 3. Generate Vector (from embedding_text)
        vector = await vector_service.generate_vector(embedding_text)
        
        resume_repo.update_resume_data(resume_id, summary_text, vector)
        
        logger.info(f"✅ [Done] Resume {resume_id} embedding updated successfully!")
        
    except Exception as e:
        logger.error(f"❌ AI Processing Failed for {resume_id}: {e}")

async def main():
    created_pairs = setup_dummy_resume_data()
    
    if created_pairs:
        logger.info(f"🚀 Starting AI Batch Processing for {len(created_pairs)} resumes...")
        # 순차 처리 (병렬 처리를 원하면 asyncio.gather 사용 가능)
        for resume_id, raw_data in created_pairs:
            await generate_and_save_embedding(resume_id, raw_data)

if __name__ == "__main__":
    asyncio.run(main())
