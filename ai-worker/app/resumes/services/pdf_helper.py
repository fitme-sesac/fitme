import os
import logging
import platform

from app.resumes.services.text_processor import TextProcessor
from app.resumes.services.layout_analyzer import LayoutAnalyzer

# 필수 라이브러리 검사 및 임포트
# 없으면 기능을 사용할 때 안내 메시지를 띄우기 위해 try-except 처리
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
    [고도화된 PDF 파싱 핸들러 (Refactored)]
    
    이 클래스는 여러 도구(`pdfplumber`, `ocr`, `TextProcessor`, `LayoutAnalyzer`)를 
    조합하여(Orchestration) 최적의 텍스트 추출 결과를 만들어냅니다.

    [핵심 전략: Hybrid Approach]
    1. 1차 시도: PDFPlumber (텍스트 레이어가 살아있는 경우 -> 속도 빠름, 정확함)
    2. 2차 시도: OCR (이미지만 있는 경우 -> 느리지만 추출 가능)
    
    [통합 파이프라인]
    1. 추출 (Extract): Raw Text 가져오기
    2. 구조화 (Analyze Layout): 헤더 제거
    3. 정제 (Process Text): 노이즈 제거, 개행 정리, 오타 교정, PII 마스킹
    4. 분리 (Sectioning): 의미 단위로 섹션 나누기
    """

    def __init__(self, tesseract_cmd_path: str = None, poppler_path: str = None):
        self.logger = logging.getLogger(__name__)
        
        # Helper 모듈 초기화 (각각 정제와 구조화를 담당)
        self.text_processor = TextProcessor()
        self.layout_analyzer = LayoutAnalyzer()
        
        # 외부 도구 경로 설정 (OCR 및 이미지 변환용)
        # 윈도우 환경에서는 설치 경로를 명시해야 할 수 있음.
        if tesseract_cmd_path:
            pytesseract.pytesseract.tesseract_cmd = tesseract_cmd_path
        self.poppler_path = poppler_path

    def extract_text(self, file_path: str) -> dict:
        """
        PDF 파일에서 텍스트를 추출하고 '쓸 수 있는 형태'로 가공하여 반환합니다.
        
        Args:
            file_path (str): PDF 파일 경로
            
        Returns:
            dict: {
                "raw_text": str,          # (디버깅용) 원본 추출 텍스트
                "cleaned_text": str,      # 헤더가 제거된 1차 정제 텍스트
                "masked_text": str,       # [최종] PII 마스킹 및 정제 완료된 텍스트
                "metadata": dict          # 마스킹된 개인정보 목록 (이메일, 전화번호 등)
            }
        """
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"PDF 파일을 찾을 수 없습니다: {file_path}")

        print(f"--- [PDFHandler] Processing: {os.path.basename(file_path)} ---")

        # ---------------------------------------------------------
        # 1단계: 텍스트 추출 (Hybrid Extraction)
        # ---------------------------------------------------------
        raw_text = self._extract_raw_text(file_path)
        
        # ---------------------------------------------------------
        # 2단계: 헤더/푸터 제거 (Layout Analysis)
        # ---------------------------------------------------------
        # 페이지 번호 등 내용과 무관한 노이즈를 먼저 쳐냅니다.
        no_header_text = self.layout_analyzer.remove_header_footer(raw_text)

        # ---------------------------------------------------------
        # 3단계: 텍스트 정제 및 보호 (Text Processing)
        # ---------------------------------------------------------
        # 노이즈 필터링 -> 개행 정리 -> 오타 교정 -> PII 마스킹 순서로 진행
        processed_result = self.text_processor.process_all(no_header_text)
        
        masked_text = processed_result["masked_text"]
        metadata = processed_result["metadata"]
        
        # [Modify] 4단계: 섹션 분리 (Deprecated)
        # LayoutAnalyzer의 섹션 감지 기능이 제거되었으므로, 해당 단계는 건너뜁니다.
        # 대신 masked_text 내의 [[Page X]] 헤더가 구조화 역할을 수행합니다.
        
        return {
            "raw_text": raw_text,       
            "cleaned_text": no_header_text, 
            "masked_text": masked_text,   # 최종 LLM 입력용 (Page 헤더 포함)
            "metadata": metadata,         # DB 저장용 (개인정보)
            # "sections": ... (Removed)
        }

    def _extract_raw_text(self, file_path: str) -> str:
        """ 
        [추출 전략 결정]
        1. 먼저 pdfplumber로 가볍게 찔러봅니다. (속도 0.1초)
        2. 텍스트가 거의 안 나오면 이미지 스캔본이라고 판단하고 OCR을 돌립니다. (속도 수 초)
        """
        
        # 1-1. pdfplumber로 텍스트 레이어 추출 시도
        extracted_text = self._extract_text_with_plumber(file_path)
        
        # [Validation] 추출된 텍스트가 '진짜 텍스트'인지 확인
        # 우리가 심은 [IMAGE_DETECTED...] 태그와 [Page ...] 헤더를 제외하고 순수 글자 수를 셉니다.
        import re
        pure_text = re.sub(r'\[IMAGE_DETECTED:.*?\]', '', extracted_text)
        pure_text = re.sub(r'\[\[Page \d+\]\]', '', pure_text).strip()
        
        # 순수 글자가 50자 넘으면 "아, 이건 텍스트 PDF구나" 하고 바로 반환
        # (주의: 빈 페이지만 잔뜩 있어서 헤더만 남은 경우 OCR로 넘기기 위함)
        # 1-2. 이미지 포함 여부 확인 및 Hybrid OCR 수행
        # 텍스트 레이어가 있지만, 이미지가 포함된 경우 OCR 데이터를 보강합니다.
        if "[IMAGE_DETECTED" in extracted_text:
            print(f"! Text found ({len(pure_text)} chars), but images also detected. Running Hybrid OCR...")
            try:
                ocr_text = self._extract_text_with_ocr(file_path)
                extracted_text += "\n\n=== [Hybrid OCR Supplement: Text from Images/Diagrams] ===\n" + ocr_text
            except Exception as e:
                print(f"Hybrid OCR Warning: OCR failed ({e}), returning only plumber text.")

        # 1-3. 텍스트가 충분하다면 반환
        if pure_text and len(pure_text) > 50:
            print(f"✓ Text layer found ({len(pure_text)} chars).")
            return extracted_text

        # 1-4. 텍스트가 너무 적으면 이미지 문서로 간주하여 OCR 수행 (Fallback)
        print(f"! Text layer insufficient ({len(pure_text)} chars). Switching to OCR strategy...")
        return self._extract_text_with_ocr(file_path)

    def _extract_text_with_plumber(self, file_path: str) -> str:
        """
        [도구: PDFPlumber]
        텍스트 레이어가 있는 PDF(워드 변환, 노션 내보내기 등)를 읽을 때 사용합니다.
        가장 정확하고 빠릅니다.
        """
        if not pdfplumber:
             print("Warning: pdfplumber not installed.")
             return ""
        
        full_text = []
        try:
            with pdfplumber.open(file_path) as pdf:
                for i, page in enumerate(pdf.pages):
                    # [핵심] layout=True, x_tolerance=1 
                    # -> 2단 컬럼(왼쪽/오른쪽 단)이 섞이지 않고 눈에 보이는 순서대로 읽도록 강제합니다.
                    text = page.extract_text(layout=True, x_tolerance=1)
                    
                    # [페이지 구분자 추가]
                    # 섹션 감지(Section Detection)가 너무 포트폴리오 특화적이라는 피드백 반영 (Step 444)
                    # 범용적인 '페이지 단위' 분리를 위해 명시적인 헤더 추가
                    page_header = f"[[Page {i+1}]]"
                    
                    # [이미지 태그 삽입]
                    if page.images:
                        meaningful_images = [img for img in page.images if img['width'] > 50 and img['height'] > 50]
                        if meaningful_images:
                            image_tag = f"\n[IMAGE_DETECTED: {len(meaningful_images)} images (diagram/chart context)]"
                            if text:
                                text += image_tag
                            else:
                                text = image_tag

                    if text:
                        # 페이지 헤더와 함께 추가
                        full_text.append(f"{page_header}\n{text}")
                    else:
                        full_text.append(page_header) # 빈 페이지라도 번호는 유지
                        
        except Exception as e:
            print(f"Error during pdfplumber extraction: {e}")
            return ""
            
        return "\n\n".join(full_text)

    def _extract_text_with_ocr(self, file_path: str) -> str:
        """
        [도구: Tesseract OCR + PDF2Image]
        이미지로 된 PDF(스캔본, 캡처본)를 읽을 때 사용합니다.
        """
        if not convert_from_path or not pytesseract:
             raise ImportError("OCR libraries missing.")

        try:
            # pdf2image: PDF를 이미지(JPG/PNG) 리스트로 변환
            images = convert_from_path(file_path, poppler_path=self.poppler_path)
            
            ocr_text = []
            for i, image in enumerate(images):
                print(f"  - OCR Processing page {i+1}/{len(images)}...")
                
                # 페이지 구분자
                page_header = f"[[Page {i+1}]]"
                
                # lang='kor+eng': 한글과 영어를 같이 인식하도록 설정
                text = pytesseract.image_to_string(image, lang='kor+eng')
                
                ocr_text.append(f"{page_header}\n{text}")
            
            return "\n\n".join(ocr_text)
            
        except Exception as e:
            print(f"Error during OCR extraction: {e}")
            raise e
