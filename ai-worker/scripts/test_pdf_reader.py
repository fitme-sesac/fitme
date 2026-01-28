import os
import sys
import argparse
# pdf 도 받는다고 가성하고 함께 요약하는 test 
# 프로젝트 루트 경로 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.resumes.services.pdf_helper import PDFHandler

# ==========================================
# [설정 완료] 사용자 환경에 맞게 경로가 설정되었습니다.
# ==========================================
# Tesseract 경로
TESSERACT_CMD_PATH = r"C:\Program Files\Tesseract-OCR\tesseract.exe"

# Poppler 경로
POPPLER_PATH = r"C:\Program Files\Release-25.12.0-0\poppler-25.12.0\Library\bin"       
# ==========================================

def get_target_file(base_dir):
    parser = argparse.ArgumentParser(description='PDF 텍스트 추출 테스트')
    parser.add_argument('filename', nargs='?', help='temp_pdfs 폴더 내의 테스트할 PDF 파일명')
    args = parser.parse_args()

    if args.filename:
        file_path = os.path.join(base_dir, args.filename)
        if os.path.exists(file_path):
            return file_path
        else:
            print(f"[오류] '{args.filename}' 파일을 찾을 수 없습니다.")

    pdf_files = [f for f in os.listdir(base_dir) if f.lower().endswith('.pdf')]
    if not pdf_files:
        print(f"[알림] '{base_dir}' 폴더에 PDF 파일이 없습니다.")
        return None

    print("\n[ 테스트할 PDF 파일을 선택해주세요 ]")
    for idx, f in enumerate(pdf_files):
        print(f"{idx + 1}. {f}")
    
    while True:
        try:
            selection = input("\n번호를 입력하세요 (엔터 = 1번): ").strip()
            if not selection:
                return os.path.join(base_dir, pdf_files[0])
            
            idx = int(selection) - 1
            if 0 <= idx < len(pdf_files):
                return os.path.join(base_dir, pdf_files[idx])
            else:
                print("잘못된 번호입니다.")
        except ValueError:
            print("숫자를 입력해주세요.")

def test_pdf_read():
    base_dir = os.path.join(os.path.dirname(__file__), "..", "temp_pdfs")
    
    target_file = get_target_file(base_dir)
    if not target_file:
        return

    print(f"\nTarget File: {target_file}")

    try:
        handler = PDFHandler(tesseract_cmd_path=TESSERACT_CMD_PATH, poppler_path=POPPLER_PATH)
        
        print("\n>>> Processing PDF...")
        # Dictionary 형태의 상세 결과 반환
        result = handler.extract_text(target_file)
        
        # 1. PII Metadata 출력
        print("\n" + "="*60)
        print(" 🛡️ [Security Check] Detected Personal Info (PII)")
        print("="*60)
        metadata = result["metadata"]
        print(f"- Emails Found: {len(metadata['emails'])} -> {metadata['emails']}")
        print(f"- Phones Found: {len(metadata['phones'])} -> {metadata['phones']}")
        print(f"- Links Found : {len(metadata['links'])} -> {metadata['links']}")

        # 2. Section Detection -> Page Separation 확인
        print("\n" + "="*60)
        print(" 📑 [Structure Check] Page Markers Detected")
        print("="*60)
        
        # 'sections'는 이제 더 이상 사용하지 않거나, LayoutAnalyzer가 Page 단위로 분리하지 않는 한 비어있을 수 있음
        # 대신 masked_text 내부에 [[Page X]] 가 있는지 확인
        if "[[Page" in result["masked_text"]:
            page_count = result["masked_text"].count("[[Page")
            print(f"✅ Found {page_count} page markers (e.g., [[Page 1]], [[Page 2]]...)")
            print("   -> The text is successfully separated by pages for the LLM.")
        else:
            print("⚠️ No page markers found. (Is this a single page or text/layout issue?)")

        # 3. Masked Text (최종 결과)
        print("\n" + "="*60)
        print(" ✅ [Final Result] Processed Text (Masked & Cleaned)")
        print("="*60)
        print(result["masked_text"])
        print("="*60)

    except ImportError as e:
        print(f"\n[설치 필요] {e}")
    except Exception as e:
        print(f"\n[Error] 처리 중 오류 발생: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_pdf_read()
