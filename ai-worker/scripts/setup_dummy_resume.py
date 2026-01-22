import asyncio
import sys
import os
import io
import json
import logging
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
    
    # 1. Member Check
    member_id = None
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT member_id FROM member LIMIT 1")
            row = cur.fetchone()
            if row:
                member_id = row[0]
                logger.info(f"✅ Found existing member ID: {member_id}")
            else:
                # Member 테이블 구조를 모르므로 임의 삽입 시도보다는 에러 처리
                # 만약 member 테이블 DDL을 알면 여기서 INSERT 가능
                try:
                    # 일반적인 컬럼 추측 (실패시 에러)
                    cur.execute("""
                        INSERT INTO member (email, name, role, status, auth_provider)
                        VALUES ('dummy_resume@test.com', 'Dummy User', 'USER', 'ACTIVE', 'EMAIL')
                        RETURNING member_id
                    """)
                    member_id = cur.fetchone()[0]
                    logger.info(f"🛠️ Created new dummy member ID: {member_id}")
                except Exception as e:
                    logger.error(f"❌ Failed to find or create member. Please ensure 'member' table exists and has data. Error: {e}")
                    return None, None
    except Exception as e:
        logger.error(f"DB Error checking member: {e}")
        return None, None

    # 2. Insert Resume (Raw SQL)
    # User Provided Data
    # file_links는 더미 리스트로 설정
    file_list = [] 
    
    mock_request = {
        # resume_id는 Auto Increment 사용
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
        "file_links": file_list,
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
    }

    resume_id = None
    try:
        with conn.cursor() as cur:
            # 2.1 Insert Main Resume
            # re_stack list -> array conversion. psycopg2 handles list as ARRAY.
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
                    0
                ) RETURNING resume_id
            """, (
                member_id,
                mock_request["basic_info"]["title"],
                mock_request["content"],
                mock_request["basic_info"]["preference"]["location"],
                mock_request["basic_info"]["preference"]["salary"],
                mock_request["basic_info"]["preference"]["employment_type"],
                re_stack_list
            ))
            resume_id = cur.fetchone()[0]
            logger.info(f"🛠️ [Insert] Resume created with ID: {resume_id}")

            # 2.2 Insert Projects
            for p in mock_request["projects"]:
                # Date format handling: "2025.10" -> "2025-10-01" (Postgres DATE type needs YYYY-MM-DD)
                # Simple parsing assuming YYYY.MM
                s_date = f"{p['start_date'].replace('.', '-')}-01"
                e_date = f"{p['end_date'].replace('.', '-')}-01" if p['end_date'] else None
                
                tech_stack_str = ", ".join(p["total_tech_stack"])
                
                # Description + Contribution combined for description column? 
                # User DDL has 'description' and 'contribution_pct' (numeric). 
                # Mock has 'contribution' (text).
                # I will append contribution text to description.
                full_desc = f"[Contribution]\n{p['contribution']}\n\n[Description]\n{p['description']}"

                cur.execute("""
                    INSERT INTO resume_project (
                        resume_id, title, start_date, end_date, tech_stack, description
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s
                    )
                """, (
                    resume_id, p["project_name"], s_date, e_date, tech_stack_str, full_desc
                ))
            logger.info(f"   - inserted {len(mock_request['projects'])} projects")

            # 2.3 Insert Careers
            for c in mock_request["careers"]:
                s_date = f"{c['start_date'].replace('.', '-')}-01"
                e_date = f"{c['end_date'].replace('.', '-')}-01" if c['end_date'] else None
                
                # Resume Career table has no description column in provided DDL.
                # Only company_name, department, role, start_date, end_date, is_current, is_verified
                # Mock has description. I will ignore description for DB insert as per DDL.
                # Must provide department. Mock doesn't have it. Use Default.
                cur.execute("""
                    INSERT INTO resume_career (
                        resume_id, company_name, department, role, start_date, end_date
                    ) VALUES (
                        %s, %s, 'N/A', %s, %s, %s
                    )
                """, (
                    resume_id, c["company_name"], c["role"], s_date, e_date
                ))
            logger.info(f"   - inserted {len(mock_request['careers'])} careers")

        conn.close() # Close sync conn, we will use async service now
        
        return resume_id, mock_request

    except Exception as e:
        logger.error(f"❌ Error inserting dummy resume: {e}")
        conn.close()
        return None, None

async def generate_and_save_embedding(resume_id, raw_data):
    """
    저장된 이력서 데이터를 바탕으로 요약을 생성하고 임베딩을 저장합니다.
    """
    if not resume_id:
        return

    logger.info(f"🔄 [AI Processing] Generating summary & embedding for Resume ID: {resume_id}...")
    
    try:
        # 1. Generate Summary (using SummaryService)
        # Service expects the dict structure we have in `raw_data`
        # Note: SummaryService calls PDFHandler if file_links exist. Ours is empty.
        
        # summary_type convert to Enum
        s_type = SummaryType(raw_data.get("summary_type", "STRUCTURED"))
        
        result_obj = await summary_service.generate_summary(
            data=raw_data, 
            type="RESUME", 
            summary_type=s_type
        )
        
        # result_obj is ResumeSummary or ResumeInsightReport Pydantic model
        
        # 2. Update DB with Formatted Summary String
        summary_text = result_obj.to_formatted_string(include_reasoning=raw_data["include_reasoning"])
        
        # [Fix] Extract Meta Data for Embedding
        meta_title = raw_data["basic_info"]["title"]
        meta_stack = ", ".join(raw_data["basic_info"]["re_stack"])
        
        embedding_text = result_obj.to_embedding_string(title=meta_title, tech_stack=meta_stack)
        
        # 3. Generate Vector
        vector = await vector_service.generate_vector(embedding_text)
        
        # 4. Save to DB (using repository)
        resume_repo.update_resume_data(resume_id, summary_text, vector)
        
        logger.info(f"✅ [Done] Resume {resume_id} embedding updated successfully!")
        
    except Exception as e:
        logger.error(f"❌ AI Processing Failed: {e}")

async def main():
    resume_id, raw_data = setup_dummy_resume_data()
    if resume_id:
        await generate_and_save_embedding(resume_id, raw_data)

if __name__ == "__main__":
    asyncio.run(main())
