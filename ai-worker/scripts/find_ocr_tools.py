import os
import shutil

def find_tools():
    print("=== OCR 도구 경로 자동 탐색기 ===")
    
    # 1. Tesseract 탐색
    print("\n🔍 Tesseract-OCR 찾는 중...")
    tesseract_candidates = [
        r"C:\Program Files\Tesseract-OCR\tesseract.exe",
        r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe",
        os.path.expanduser(r"~\AppData\Local\Tesseract-OCR\tesseract.exe")
    ]
    
    tesseract_found = None
    
    # PATH 환경변수 확인
    if shutil.which("tesseract"):
        print("✅ PATH 환경변수에서 'tesseract'를 찾았습니다. (설정 불필요)")
        tesseract_found = "PATH"
    else:
        for path in tesseract_candidates:
            if os.path.exists(path):
                print(f"✅ 발견됨: {path}")
                tesseract_found = path
                break
    
    if not tesseract_found:
        print("❌ Tesseract를 찾지 못했습니다. 설치가 필요하거나 경로가 특이할 수 있습니다.")

    # 2. Poppler 탐색
    print("\n🔍 Poppler (pdftoppm/pdfinfo) 찾는 중...")
    # Poppler는 보통 bin 폴더를 PATH에 등록하거나 폴더째로 둡니다.
    # 일반적인 설치 경로 예상이 어려우므로 PATH 확인이 우선입니다.
    
    poppler_found = None
    if shutil.which("pdftoppm"):
        print("✅ PATH 환경변수에서 'pdftoppm'을 찾았습니다. (설정 불필요)")
        poppler_found = "PATH"
    else:
        # 흔한 설치 위치 (Chocolatey, conda 등)
        poppler_candidates = [
            r"C:\Program Files\poppler\bin",
            r"C:\Program Files\poppler-24.02.0\Library\bin", # 예시 버전
            r"C:\Program Files (x86)\poppler\bin"
        ]
        # Program Files 아래 poppler로 시작하는 폴더 검색 시도
        try:
             prog_files = r"C:\Program Files"
             if os.path.exists(prog_files):
                 for d in os.listdir(prog_files):
                     if "poppler" in d.lower():
                         bin_path = os.path.join(prog_files, d, "Library", "bin")
                         if os.path.exists(bin_path):
                             poppler_candidates.append(bin_path)
                         else:
                             # Release 구조인 경우
                             bin_path_simple = os.path.join(prog_files, d, "bin")
                             if os.path.exists(bin_path_simple):
                                 poppler_candidates.append(bin_path_simple)
        except:
            pass

        for path in poppler_candidates:
            # bin 폴더 안에 pdftoppm.exe가 있는지 확인
            check_exe = os.path.join(path, "pdftoppm.exe")
            if os.path.exists(check_exe):
                print(f"✅ 발견됨: {path}")
                poppler_found = path
                break
                
    if not poppler_found:
        print("❌ Poppler를 찾지 못했습니다.")

    print("\n" + "="*40)
    print(" [결과 요약 및 적용 방법]")
    print("="*40)
    
    if tesseract_found:
        if tesseract_found == "PATH":
            print("1. Tesseract: 이미 시스템에 등록되어 있습니다. 코드에서 TESSERACT_CMD_PATH = None 그대로 두세요.")
        else:
            print(f"1. Tesseract: 아래 내용을 복사해서 코드에 붙여넣으세요.")
            print(f"   TESSERACT_CMD_PATH = r\"{tesseract_found}\"")
    else:
        print("1. Tesseract: 설치가 필요해 보입니다. (https://github.com/UB-Mannheim/tesseract/wiki)")

    print("-" * 20)

    if poppler_found:
        if poppler_found == "PATH":
            print("2. Poppler: 이미 시스템에 등록되어 있습니다. 코드에서 POPPLER_PATH = None 그대로 두세요.")
        else:
            print(f"2. Poppler: 아래 내용을 복사해서 코드에 붙여넣으세요.")
            print(f"   POPPLER_PATH = r\"{poppler_found}\"")
    else:
        print("2. Poppler: 설치 또는 경로 확인이 필요합니다.")
        print("   (다운로드: Release 최신 버전 압축 해제 -> bin 폴더 경로 복사)")

if __name__ == "__main__":
    find_tools()
