import os
import sys
import argparse

# 프로젝트 루트 경로 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.resumes.services.pdf_helper import PDFHandler

# ==========================================
# [설정 완료] 사용자 환경에 맞게 경로가 설정되었습니다.
# ==========================================
# 설치된 프로그램의 경로를 지정해야 OCR이 작동합니다.

# Tesseract 경로 (탐색 스크립트로 발견됨)
TESSERACT_CMD_PATH = r"C:\Program Files\Tesseract-OCR\tesseract.exe"

# Poppler 경로 (사용자 직접 입력)
POPPLER_PATH = r"C:\Program Files\Release-25.12.0-0\poppler-25.12.0\Library\bin"       
# ==========================================

def get_target_file(base_dir):
    # 1. 커맨드라인 인자로 파일명 받기
    parser = argparse.ArgumentParser(description='PDF 텍스트 추출 테스트')
    parser.add_argument('filename', nargs='?', help='temp_pdfs 폴더 내의 테스트할 PDF 파일명')
    args = parser.parse_args()

    if args.filename:
        # 인자로 받은 파일이 있는지 확인
        file_path = os.path.join(base_dir, args.filename)
        if os.path.exists(file_path):
            return file_path
        else:
            print(f"[오류] '{args.filename}' 파일을 찾을 수 없습니다.")
            # 파일이 없으면 아래 목록 보여주기로 넘어감

    # 2. 파일 목록 보여주고 선택받기
    pdf_files = [f for f in os.listdir(base_dir) if f.lower().endswith('.pdf')]
    
    if not pdf_files:
        print(f"[알림] '{base_dir}' 폴더에 PDF 파일이 없습니다.")
        return None

    print("\n[ 테스트할 PDF 파일을 선택해주세요 ]")
    for idx, f in enumerate(pdf_files):
        print(f"{idx + 1}. {f}")
    
    while True:
        try:
            selection = input("\n번호를 입력하세요 (그냥 엔터치면 1번 선택): ").strip()
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
    
    if not os.path.exists(base_dir):
        os.makedirs(base_dir)
        print(f"[알림] '{base_dir}' 폴더가 생성되었습니다. 파일을 넣어주세요.")
        return

    # 파일 선택
    target_file = get_target_file(base_dir)
    if not target_file:
        return

    print(f"\nTarget File: {target_file}")

    # 핸들러 초기화 및 실행
    try:
        handler = PDFHandler(tesseract_cmd_path=TESSERACT_CMD_PATH, poppler_path=POPPLER_PATH)
        
        print("\n>>> Extracting text...")
        result_text = handler.extract_text(target_file)
        
        print("\n" + "="*50)
        print(" [Extraction Result] ")
        print("="*50)
        print(result_text)
        print("="*50)
        print(f"\nTotal Length: {len(result_text)} characters")
        
    except ImportError as e:
        print(f"\n[설치 필요] {e}")
        print("OCR 기능을 사용하려면 운영체제에 Tesseract와 Poppler 프로그램을 별도로 설치해야 합니다.")
    except Exception as e:
        print(f"\n[Error] 처리 중 오류 발생: {e}")

if __name__ == "__main__":
    test_pdf_read()
