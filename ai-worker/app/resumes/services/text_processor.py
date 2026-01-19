import re
import logging

class TextProcessor:
    """
    텍스트 정제, PII(개인정보) 마스킹, OCR 보정 등 문자열 처리 전용 클래스.
    
    [주요 기능]
    1. OCR 노이즈 필터링: 의미 없는 특수문자, 라인 제거
    2. 텍스트 정제: 끊긴 문장 연결, 문단 구분 표준화
    3. 오타 교정: OCR에서 자주 틀리는 기술/도메인 용어 교정
    4. PII 마스킹: 이메일, 전화번호 등 민감 정보 가림 처리
    """

    def __init__(self):
        self.logger = logging.getLogger(__name__)
        
        # OCR 오타 교정 사전 (범용적 구조적 오류만 교정)
        # 특정 기술 스택 오타(Javs -> Java 등)는 LLM이 문맥을 통해 해결하도록 위임합니다.
        # 여기서는 LLM도 헷갈리기 쉬운 '기호적 유사성'에 의한 오류만 잡습니다.
        self.ocr_corrections = {
            r"\bAl\b": "AI",          # Al(에이엘) -> AI(에이아이) : 폰트 문제로 매우 빈번
            r"\b0penAI\b": "OpenAI",  # 0(숫자) -> O(알파벳)
            r"\bl\b": "I",            # l(소문자 엘) -> I(대문자 아이) : 상황에 따라 다르지만 LLM Tokenizer가 혼동하기 쉬워 전처리
        }

    def filter_ocr_noise(self, text: str) -> str:
        """
        [OCR 결과 노이즈 제거 필터]
        OCR로 추출된 텍스트 중 분석에 방해되는 '쓰레기 데이터'를 1차적으로 걸러냅니다.
        
        [해결하는 문제]
        - Issue 2.6: 오인식된 노이즈
        - Issue 6.1~6.3: 의미 없는 문자열 나열
        
        Args:
            text (str): 원본 텍스트
            
        Returns:
            str: 노이즈가 제거된 텍스트
        """
        lines = text.split('\n')
        filtered_lines = []
        
        for line in lines:
            line = line.strip()
            if not line:
                continue

            # 1. 의미 없는 짧은 줄 제거
            # 예: "o", "=", "<" 등 OCR 잔여물
            # (한글, 영문, 숫자가 하나도 없으면 무조건 노이즈로 간주)
            if len(line) < 3 and not re.search(r'[가-힣A-Za-z0-9]', line):
                continue
                
            # 2. 특수문자로만 구성된 줄 제거
            # 예: "| | | |", "......" 등 표나 선의 잔해
            if re.fullmatch(r'[\W_]+', line):
                continue

            # 3. 반복 기호 제거 (줄 내부)
            # 예: "------", "======" 등 구분선이 텍스트로 인식된 경우 삭제
            line = re.sub(r'[|_=~\-]{3,}', '', line).strip()
            if not line:
                continue
            
            # 4. 유효 텍스트 비율 체크
            # 한글/영문/숫자가 너무 적은 줄은 정보가 없는 것으로 판단 (이미지/아이콘 오인식 등)
            hangul_count = len(re.findall(r'[가-힣]', line))
            alpha_count = len(re.findall(r'[A-Za-z]', line))
            digit_count = len(re.findall(r'[0-9]', line))
            
            # 단, [IMAGE_DETECTED...] 태그는 우리가 심은 것이므로 살려둬야 함.
            if line.startswith('[') and line.endswith(']'):
                pass
            # 유효 글자(한글+영문+숫자) 합이 3자 미만이면 삭제
            elif (hangul_count + alpha_count + digit_count) < 3:
                continue

            filtered_lines.append(line)
        
        return '\n'.join(filtered_lines)

    def clean_text(self, text: str) -> str:
        """
        [텍스트 정제]
        문장 구조를 복원하기 위한 전처리 단계입니다.
        
        [해결하는 문제]
        - PDF에서 문장이 줄바꿈(\n)으로 뚝뚝 끊기는 현상 (Broken Lines)
        - 문단과 문단 사이의 구분을 명확히 하기 위함
        """
        if not text:
            return ""

        # 1. 윈도우 스타일 개행(\r\n)을 유닉스 스타일(\n)로 통일
        text = text.replace('\r\n', '\n')

        # 2. 다중 개행(빈 줄) 보호
        # \n이 2번 이상이면 문단이 바뀐 것이므로, 임시 마커(<PARAGRAPH_BREAK>)로 바꿔서 보호합니다.
        text = re.sub(r'\n{2,}', ' <PARAGRAPH_BREAK> ', text)

        # 3. 단일 개행 제거 (Line Merging)
        # 문장 중간에 있는 단순 줄바꿈은 공백으로 바꿔서 문장을 이어줍니다.
        # 예: "안녕하세요\n반갑습니다" -> "안녕하세요 반갑습니다"
        text = text.replace('\n', ' ')

        # 4. 임시 마커 복원
        # 아까 보호해둔 문단 바꿈을 다시 \n\n으로 되돌립니다.
        text = text.replace('<PARAGRAPH_BREAK>', '\n\n')

        # 5. 연속 공백 제거
        text = re.sub(r' +', ' ', text).strip()
        
        return text

    def mask_pii(self, text: str) -> dict:
        """
        [개인정보 보호 (PII Masking)]
        이력서 분석 시 민감할 수 있는 연락처 정보를 마스킹하고, 별도로 추출합니다.
        (LLM에 개인정보가 넘어가는 것을 방지)
        
        Returns:
            dict: {
                "masked_text": 마스킹된 본문,
                "metadata": { "emails": [...], "phones": [...], "links": [...] }
            }
        """
        metadata = {
            "emails": [],
            "phones": [],
            "links": []
        }

        # 1. Email 패턴
        # 예: user.name@domain.co.kr
        email_pattern = r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}'
        
        def email_repl(match):
            metadata["emails"].append(match.group())
            return "[EMAIL]"
            
        text = re.sub(email_pattern, email_repl, text)

        # 2. Phone 패턴 (국내 번호 위주)
        # 예: 010-1234-5678, 02-123-4567
        phone_pattern = r'\b(010|02|031|032|033|041|042|043|044|051|052|053|054|055|061|062|063|064)-\d{3,4}-\d{4}\b'
        
        def phone_repl(match):
            metadata["phones"].append(match.group())
            return "[PHONE]"
            
        text = re.sub(phone_pattern, phone_repl, text)

        # 3. URL 링크 패턴
        # http가 없어도 도메인 형태면 감지 (github.com 등)
        url_pattern = r'(https?://|www\.)[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}[^\s]*'
        
        def url_repl(match):
            metadata["links"].append(match.group())
            return "[LINK]"
            
        text = re.sub(url_pattern, url_repl, text)

        return {
            "masked_text": text,
            "metadata": metadata
        }

    def correct_ocr_errors(self, text: str) -> str:
        """
        [OCR 오타 교정]
        미리 정의된 사전(self.ocr_corrections)을 이용하여 
        자주 틀리는 단어를 올바르게 고칩니다.
        """
        for pattern, replacement in self.ocr_corrections.items():
            text = re.sub(pattern, replacement, text)
        return text

    def process_all(self, text: str) -> dict:
        """
        [텍스트 처리 파이프라인 실행]
        정해진 순서대로 텍스트를 처리합니다.
        
        순서:
        1. OCR 노이즈 필터링 (쓰레기 값 제거)
        2. 텍스트 정제 (줄글로 만들기)
        3. 오타 교정 (키워드 복원)
        4. PII 마스킹 (보안 처리)
        """
        # 1. OCR 노이즈 필터링
        text = self.filter_ocr_noise(text)

        # 2. 텍스트 정제 (개행 정리)
        cleaned = self.clean_text(text)
        
        # 3. 오타 교정
        corrected = self.correct_ocr_errors(cleaned)
        
        # 4. PII 마스킹
        result = self.mask_pii(corrected)
        
        return result
