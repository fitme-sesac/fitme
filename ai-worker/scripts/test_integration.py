
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

def get_first_pdf():
    """temp_pdfs 폴더에서 첫 번째 PDF 파일을 찾아 반환합니다."""
    if not os.path.exists(TEMP_PDF_DIR):
        print(f"❌ 폴더가 없습니다: {TEMP_PDF_DIR}")
        return None
        
    files = [f for f in os.listdir(TEMP_PDF_DIR) if f.endswith('.pdf')]
    if files:
        return os.path.join(TEMP_PDF_DIR, files[0])
    return None

async def test_integration():
    print("\n[Step 1] 테스트용 PDF 파일 검색...")
    pdf_path = get_first_pdf()
    if not pdf_path:
        print("❌ 테스트할 PDF 파일이 없습니다. 'temp_pdfs' 폴더에 PDF를 넣어주세요.")
        return

    print(f"📄 Found PDF: {pdf_path}")
    
    # --------------------------------------------------------------------------
    # [Step 2] API 요청 데이터 시뮬레이션 (Mock Data)
    # 실제 프론트엔드에서 보낼법한 JSON 데이터를 흉내냅니다.
    # 'file_links' 에 PDF 경로 목록을 넣어주면, 서비스가 알아서 읽습니다.
    # --------------------------------------------------------------------------
    print("\n[Step 2] 가상 요청 데이터(Mock Request) 생성 중...")
    
    # 똑같은 파일을 2번 넣어서 "다중 파일 처리"가 잘 되는지 확인합니다.
    file_list = [pdf_path, pdf_path] 
    
    mock_request = {
        "resume_id": 999, # 테스트용 임의 ID
        "basic_info": {
            "title": "AI Engineer Test",
            "tagline": "Passionate Developer",
            "re_stack": ["Python", "FastAPI", "LangChain"], # 더미 데이터
            "field": "RESUME",
            "preference": {
                "location": "Remote",
                "salary": "Negotiable",
                "employment_type": "Full-time"
            }
        },
        "content": """
        [자기소개서 본문]
        저는 백엔드 개발자입니다. 하지만 이 내용은 크게 중요하지 않습니다.
        왜냐하면 지금은 첨부된 PDF 파일의 내용이 제대로 반영되는지 테스트 중이기 때문입니다.
        LLM이 이 텍스트와 PDF 내용을 합쳐서 요약해줄 것입니다.
        """,
        "file_links": file_list, # [핵심] 여기에 PDF 경로 리스트 전달
        "projects": [],
        "careers": [],
        "summary_type": "STRUCTURED" # 구조화된 결과를 요청
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
            summary_type=SummaryType.STRUCTURED
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
