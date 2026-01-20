import re
from typing import List, Dict

class LayoutAnalyzer:
    """
    [구조적 노이즈 제거 (Structural Noise Cleaner)]
    
    '페이지 분리' 기능이 PDFHandler로 이관됨에 따라, 
    현재는 **페이지마다 반복되는 불필요한 영역(Header/Footer/Page Numbers)을 제거**하는 역할에 집중합니다.
    
    [역할 구분]
    - TextProcessor: OCR 노이즈, 특수문자, PII 등 '컨텐츠 내부'의 오염을 정제
    - LayoutAnalyzer: 쪽 번호, 회사명 등 '문서 구조적'으로 반복되는 노이즈를 제거
    """

    def __init__(self):
        # [변경] 기존 섹션 키워드 제거 (페이지 단위 분리 전략으로 변경됨에 따라 불필요)
        pass

    def remove_header_footer(self, text: str) -> str:
        """
        [헤더/푸터 제거]
        OCR이나 PDF 추출 시 페이지 상/하단에 딸려오는 페이지 번호 등을 제거합니다.
        (Issue 2.5, 4.5 해결 목적)
        
        Args:
            text (str): 전체 텍스트
            
        Returns:
            str: 헤더/푸터가 제거된 텍스트
        """
        lines = text.split('\n')
        cleaned_lines = []
        
        # 페이지 번호 패턴 (정규식)
        # 예: "1 / 4", "- 1 -", "Page 1 of 10", "1" (단독 숫자)
        # 주의: [[Page X]] 형식은 괄호 때문에 이 패턴에 걸리지 않으므로 안전함 (검증 완료)
        page_num_pattern = re.compile(
            r'^\s*(-?\s*\d+\s*-?|\d+\s*/\s*\d+|Page\s*\d+(\s*of\s*\d+)?)\s*$', 
            re.IGNORECASE
        )
        
        for line in lines:
            line_str = line.strip()
            
            # 1. 페이지 번호 패턴과 일치하면 스킵 (삭제)
            if page_num_pattern.match(line_str):
                continue
            
            cleaned_lines.append(line)
            
        return '\n'.join(cleaned_lines)
