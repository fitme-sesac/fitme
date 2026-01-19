
# ------------------------------------------------------------------------------
# [통합 테스트 스크립트] PDF 요약 파이프라인 검증
# ------------------------------------------------------------------------------
# 이 스크립트는 "PDF 파일 입력 -> 텍스트 추출 -> 전처리 -> LLM 요약"까지의
# 전체 과정을 한 번에 테스트하는 도구입니다.
#
# Q: 이 파일만 실행하면 요약까지 다 받는 건가요?
# A: 네, 맞습니다! `summary_service.generate_summary()`를 호출하여
#    실제로 OpenAI API를 통해 생성된 요약 결과를 출력합니다.
# ------------------------------------------------------------------------------

import asyncio
import os
import sys

# 프로젝트 루트 경로 추가 (모듈 import를 위해 필요)
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.resumes.services.summary import summary_service, SummaryType

# 테스트할 파일이 있는 디렉토리 (temp_pdfs 폴더)
TEMP_PDF_DIR = os.path.join(os.path.dirname(__file__), "..", "temp_pdfs")

def select_files():
    """temp_pdfs 폴더에서 사용자가 다양한 파일을 선택하게 합니다."""
    if not os.path.exists(TEMP_PDF_DIR):
        print(f"❌ 폴더가 없습니다: {TEMP_PDF_DIR}")
        return []
        
    files = [f for f in os.listdir(TEMP_PDF_DIR) if f.lower().endswith('.pdf')]
    if not files:
        print(f"❌ '{TEMP_PDF_DIR}' 폴더에 PDF 파일이 없습니다.")
        return []

    print("\n[ 테스트할 PDF 파일을 선택해주세요 (업로드 시뮬레이션) ]")
    for idx, f in enumerate(files):
        print(f"{idx + 1}. {f}")
    
    print("0. 선택 안함 (PDF 없이 텍스트만 테스트)")
    print("\n예시) 1, 2 입력 시 1번과 2번 파일 모두 선택")
    selection = input("번호를 입력하세요 (쉼표로 구분): ").strip()
    
    selected_files = []
    if not selection:
        # 아무것도 입력 안 하면 첫 번째 파일만 선택
        print("-> 기본값(첫 번째 파일)을 선택합니다.")
        return [os.path.join(TEMP_PDF_DIR, files[0])]
    
    try:
        if selection == '0':
            return []
            
        indices = [int(x.strip()) - 1 for x in selection.split(',')]
        for idx in indices:
            if 0 <= idx < len(files):
                selected_files.append(os.path.join(TEMP_PDF_DIR, files[idx]))
            else:
                print(f"⚠️ 잘못된 번호 무시됨: {idx + 1}")
    except ValueError:
        print("⚠️ 숫자 입력 오류. 기본값으로 진행합니다.")
        return [os.path.join(TEMP_PDF_DIR, files[0])]
        
    return selected_files

async def test_integration():
    print("\n[Step 1] 테스트용 PDF 파일 검색...")
    
    # [Modify] 사용자 지정 2개 이상의 파일 선택 기능 추가 (Step 586)
    target_files = select_files()
    
    if not target_files:
        print("👉 선택된 파일이 없습니다. (PDF 없이 텍스트 데이터만으로 테스트합니다)")
    else:
        print(f"📄 Selected Files ({len(target_files)}개):")
        for f in target_files:
            print(f"   - {os.path.basename(f)}")
    
    # [Check] AI Reasoning 옵션
    use_reasoning = input("\n[Option] 결과에 'AI 분석 근거'를 포함하시겠습니까? (y/n) [default: y]: ").strip().lower()
    include_reasoning = True if use_reasoning in ['y', 'yes', ''] else False

    # [Check] 요약 형태 선택
    print("\n[Option] 요약 형태를 선택하세요:")
    print("1. STRUCTURED (기존 8줄 요약)")
    print("2. REPORT (신규 인사이트 보고서)")
    print("3. TEXT (줄글 추천사)")
    mode_choice = input("번호 입력 [default: 2]: ").strip()
    
    if mode_choice == '1':
        summary_type = "STRUCTURED"
    elif mode_choice == '2':
        summary_type = "REPORT"
    else:
        summary_type = "TEXT"
        
    print(f"👉 선택된 모드: {summary_type}")

    # --------------------------------------------------------------------------
    # [Step 2] API 요청 데이터 시뮬레이션 (Mock Data)
    # 실제 프론트엔드에서 보낼법한 JSON 데이터를 흉내냅니다.
    # 'file_links' 에 PDF 경로 목록을 넣어주면, 서비스가 알아서 읽습니다.
    # --------------------------------------------------------------------------
    print("\n[Step 2] 가상 요청 데이터(Mock Request) 생성 중...")
    
    # 사용자가 선택한 파일 리스트를 그대로 전달
    file_list = target_files
    
    mock_request = {
        "resume_id": 2,
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
        "file_links": file_list, # [핵심] 여기에 PDF 경로 리스트 전달
        "projects": [
            {
                "project_name": "유튜브 영상 컨텐츠 추천 서비스 개발",
                "start_date": "2025.10",
                "end_date": "2025.12",
                "total_tech_stack": ["Spring Boot", "MongoDB", "Qdrant(Vector DB)", "OpenAI LLM", "RAG", "Docker"],
                "contribution": "유튜브 영상의 소비 패턴을 분석하여 개인화된 세부 카테고리 기반 분류를 구현함으로써, 사용자 경험과 콘텐츠 소비의 질을 개선",
                "description": """
                [문제] 기존 추천 서비스는 단순 키워드 매칭 기반이라 개인화 부족
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
                "contribution": """
                확장성 문제 해결: 고성능 MSA 기반 주문 처리
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
                "description": "업무 초기 어려움을 극복하고 문서화 작업을 통해 후임자 인수인계와 업무 효율 향상, 낯선 업무 환경에서 효율적인 적응을 위해 업무 프로세스를 정리하고 문서화, 근로 기간 중 업무 효율을 높이고, 후임자에게 도움이 되는 인수인계 자료를 제공 "
            }
        ],
        "summary_type": summary_type, # 선택된 모드 주입
        "include_reasoning": include_reasoning # [NEW] 근거 포함 여부
    }
    
    # --------------------------------------------------------------------------
    # [Step 3] Summary Service 호출 (핵심 로직 실행)
    # 여기서 PDF 추출 -> 전처리 -> 프롬프트 조합 -> OpenAI 호출이 모두 일어납니다.
    # --------------------------------------------------------------------------
    print(f"\n🚀 [Step 3] SummaryService 호출! (PDF {len(file_list)}개 처리 중...)")
    print("   -> PDF 텍스트 추출 및 전처리")
    print("   -> LLM 요약 생성 (약 5~10초 소요 예정)...")
    
    try:
        # 서비스 호출 (비동기)
        result = await summary_service.generate_summary(
            data=mock_request, 
            type="RESUME", 
            summary_type=SummaryType[summary_type] # String -> Enum 변환
        )
        
        # ----------------------------------------------------------------------
        # [Step 4] 최종 결과 출력
        # 성공하면 여기에 LLM이 만든 요약문이 찍힙니다.
        # ----------------------------------------------------------------------
        print("\n✅ [Success] 통합 테스트 성공! 아래는 LLM이 생성한 결과입니다:")
        print("="*80)
        print(result) # 실제 요약 결과
        print("="*80)
        
    except Exception as e:
        print(f"\n❌ [Error] 테스트 실패: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # 비동기 함수 실행을 위한 이벤트 루프 시작
    asyncio.run(test_integration())
