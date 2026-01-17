import os
import logging
import platform

# 라이브러리 임포트 (설치 여부 확인을 위해 try-except 사용 가능하지만, 여기서는 필수라고 가정)
try:
    import pdfplumber
except ImportError:
    pdfplumber = None

try:
    from pdf2image import convert_from_path
except ImportError:
    convert_from_path = None

try:
    import pytesseract
except ImportError:
    pytesseract = None

class PDFHandler:
    """
    PDF 파일에서 텍스트를 추출하는 핸들러 클래스.
    Hybrid 접근 방식을 사용합니다:
    1. PDFPlumber로 텍스트 직접 추출 시도 (빠르고 정확)
    2. 추출된 텍스트가 없거나 너무 적으면(이미지형 PDF), Tesseract OCR 로 전환
    """

    def __init__(self, tesseract_cmd_path: str = None, poppler_path: str = None):
        self.logger = logging.getLogger(__name__)
        
        # Windows 환경 설정 (Tesseract 경로가 명시된 경우 설정)
        if tesseract_cmd_path:
            pytesseract.pytesseract.tesseract_cmd = tesseract_cmd_path
        
        # Poppler 경로 (Windows에서 pdf2image 사용 시 필요할 수 있음)
        self.poppler_path = poppler_path

    def extract_text(self, file_path: str) -> str:
        """
        주어진 PDF 파일 경로에서 텍스트를 추출합니다.
        """
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"PDF 파일을 찾을 수 없습니다: {file_path}")

        print(f"--- [PDFHandler] Processing: {os.path.basename(file_path)} ---")

        # 1단계: 텍스트 레이어 직접 추출 (PDFPlumber)
        extracted_text = self._extract_text_with_plumber(file_path)
        
        # 텍스트가 충분한지 확인 (예: 공백 제외 50자 이상)
        if extracted_text and len(extracted_text.strip()) > 50:
            print(f"✓ Text layer found ({len(extracted_text)} chars). Skipping OCR.")
            return extracted_text

        # 2단계: 텍스트가 없으면 OCR 수행 (pdf2image + Tesseract)
        print(f"! Text layer insufficient. Switching to OCR strategy...")
        return self._extract_text_with_ocr(file_path)

    def _extract_text_with_plumber(self, file_path: str) -> str:
        if not pdfplumber:
             print("Warning: pdfplumber not installed.")
             return ""
        
        full_text = []
        try:
            with pdfplumber.open(file_path) as pdf:
                for page in pdf.pages:
                    text = page.extract_text()
                    if text:
                        full_text.append(text)
        except Exception as e:
            print(f"Error during pdfplumber extraction: {e}")
            return ""
            
        return "\n".join(full_text)

    def _extract_text_with_ocr(self, file_path: str) -> str:
        if not convert_from_path:
            raise ImportError("`pdf2image` 라이브러리가 설치되지 않았습니다. pip install pdf2image")
        if not pytesseract:
             raise ImportError("`pytesseract` 라이브러리가 설치되지 않았습니다. pip install pytesseract")

        try:
            # PDF를 이미지 리스트로 변환
            # Windows의 경우 poppler_path가 필요할 수 있음
            images = convert_from_path(file_path, poppler_path=self.poppler_path)
            
            ocr_text = []
            for i, image in enumerate(images):
                print(f"  - OCR Processing page {i+1}/{len(images)}...")
                # 한국어+영어 추출 (lang='kor+eng')
                # 주의: Tesseract에 한국어 데이터(kor.traineddata)가 설치되어 있어야 함
                text = pytesseract.image_to_string(image, lang='kor+eng')
                ocr_text.append(text)
            
            return "\n".join(ocr_text)
            
        except Exception as e:
            print(f"Error during OCR extraction: {e}")
            raise e
