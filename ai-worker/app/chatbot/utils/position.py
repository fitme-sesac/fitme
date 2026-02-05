# app/chatbot/utils/position.py
from __future__ import annotations

import re
from typing import Dict, List, Optional, Set

# NOTE: 이 매핑은 backend/common/util/JobPositionUtil.java 의 POSITION_KEYWORDS / POSITION_ORDER 를
#       챗봇에서 동일하게 재현한 것입니다. (챗봇만 수정하는 요구사항에 따라 로컬로 복제)

POSITION_ORDER: List[str] = ['전체', '서버/백엔드', '프론트엔드', '웹 풀스택', '안드로이드', 'iOS', '크로스플랫폼', '머신러닝/AI', '데이터 엔지니어', '데이터 분석가', '데이터 사이언티스트', 'DevOps', '시스템 엔지니어', '클라우드 엔지니어', 'DBA', 'SRE', '보안 엔지니어', '게임 클라이언트', '게임 서버', '임베디드', '시스템 프로그래머', 'QA 엔지니어', '기술 PM', '프로덕트 매니저', 'UX/UI 디자이너', '블록체인']

POSITION_KEYWORDS: Dict[str, Set[str]] = {
    '서버/백엔드': {'java', 'spring', 'springboot', 'kotlin', 'node', 'nodejs', 'python', 'django', 'flask', 'fastapi', 'go', 'golang', 'ruby', 'rails', 'php', 'c#', 'csharp', '.net', 'asp.net', 'backend', 'back-end', '백엔드', '서버', 'express', 'nest', 'nestjs', 'mysql', 'postgresql', 'mongodb', 'redis', 'graphql', 'rest api', 'microservice', '마이크로서비스'},
    '프론트엔드': {'react', 'vue', 'angular', 'javascript', 'typescript', 'next.js', 'nextjs', 'nuxt', 'svelte', 'remix', 'html', 'css', 'scss', 'sass', 'frontend', 'front-end', '프론트엔드', '프론트', 'tailwind', 'styled-components', 'webpack', 'vite', '웹퍼블리셔', '퍼블리셔'},
    '웹 풀스택': {'fullstack', 'full-stack', '풀스택', 'full stack'},
    '안드로이드': {'android', '안드로이드', 'kotlin', 'java', 'jetpack', 'compose', '안드로이드 개발'},
    'iOS': {'ios', 'swift', 'objective-c', 'xcode', '아이폰', 'swiftui', 'uikit', 'ios 개발'},
    '크로스플랫폼': {'flutter', 'react native', 'reactnative', 'dart', 'xamarin', 'ionic', 'cordova', '하이브리드앱'},
    '머신러닝/AI': {'tensorflow', 'pytorch', 'machine learning', 'ml', 'ai', 'keras', '머신러닝', '딥러닝', 'deep learning', 'nlp', 'computer vision', 'huggingface', 'llm', 'gpt', '인공지능', 'scikit-learn', 'opencv', 'langchain', 'prompt engineering', '생성형 ai'},
    '데이터 엔지니어': {'spark', 'airflow', 'kafka', 'bigquery', 'data engineer', '데이터 엔지니어', 'etl', 'hadoop', 'hive', 'presto', 'databricks', 'snowflake', 'redshift', '데이터 파이프라인', 'data pipeline'},
    '데이터 분석가': {'data analyst', '데이터 분석', 'bi', 'tableau', 'power bi', 'looker', 'sql', 'analytics', '통계', 'statistics', 'r', '사업분석', 'business analyst'},
    '데이터 사이언티스트': {'data scientist', '데이터 사이언티스트', '데이터 과학', 'predictive modeling', 'a/b test', '실험 설계', '추천 시스템', 'recommendation'},
    'DevOps': {'docker', 'kubernetes', 'k8s', 'terraform', 'ci/cd', 'devops', 'aws', 'gcp', 'azure', 'jenkins', 'github actions', 'gitlab ci', 'argocd', 'helm', 'ansible', 'puppet', 'chef'},
    '시스템 엔지니어': {'system engineer', '시스템 엔지니어', 'linux', 'unix', 'network', '네트워크', 'infra', '인프라', 'vmware', '시스템 관리', '서버 관리'},
    '클라우드 엔지니어': {'cloud engineer', '클라우드 엔지니어', 'aws', 'gcp', 'azure', 'cloud', '클라우드', 'lambda', 'serverless', '서버리스', 'eks', 'ecs', 'fargate'},
    'DBA': {'dba', 'database administrator', '데이터베이스 관리자', 'mysql dba', 'postgresql dba', 'oracle', 'mssql', 'db 관리', 'database', 'db 튜닝'},
    'SRE': {'sre', 'site reliability', '사이트 신뢰성', 'monitoring', '모니터링', 'prometheus', 'grafana', 'observability', 'incident', '장애 대응'},
    '보안 엔지니어': {'security', '보안', 'penetration', 'pentesting', 'vulnerability', '취약점', '정보보안', '침투 테스트', '보안 취약점', '시큐리티', 'cybersecurity', 'soc', 'cert'},
    '게임 클라이언트': {'unity', 'unreal', 'game', '게임', 'cocos', 'godot', '게임 개발', 'c++', '게임 클라이언트'},
    '게임 서버': {'game server', '게임 서버', 'photon', 'mirror', 'netcode', '멀티플레이어', 'multiplayer'},
    '임베디드': {'embedded', '임베디드', 'firmware', '펌웨어', 'rtos', 'arm', 'mcu', '아두이노', '라즈베리파이', 'stm32', 'esp32', 'iot', '사물인터넷'},
    '시스템 프로그래머': {'system programmer', '시스템 프로그래머', 'c', 'c++', 'rust', 'low-level', '커널', 'kernel', 'driver', '드라이버', 'os'},
    'QA 엔지니어': {'qa', 'quality assurance', '품질 보증', 'test', '테스트', 'automation test', '자동화 테스트', 'selenium', 'cypress', 'playwright', 'appium', '테스트 엔지니어', 'sdet'},
    '기술 PM': {'technical pm', '기술 pm', 'tpm', 'tech lead', '테크 리드', '개발팀장', 'engineering manager', '엔지니어링 매니저', 'agile', 'scrum', '스크럼 마스터'},
    '프로덕트 매니저': {'product manager', 'pm', '프로덕트 매니저', '서비스 기획', 'product owner', 'po', '프로덕트 오너', '기획자', '서비스 기획자'},
    'UX/UI 디자이너': {'ux', 'ui', 'ux/ui', 'ui/ux', '디자이너', 'designer', 'figma', 'sketch', 'adobe xd', '프로토타이핑', 'interaction design', '인터랙션 디자인'},
    '블록체인': {'blockchain', '블록체인', 'solidity', 'web3', 'smart contract', '스마트 컨트랙트', 'ethereum', '이더리움', 'defi', 'nft', 'crypto'},
}

_POSITION_PATTERNS: List[tuple[str, re.Pattern]] = [
    ("서버/백엔드", re.compile(r"(백엔드|백앤드|서버|backend|back[- ]?end)", re.IGNORECASE)),
    ("프론트엔드", re.compile(r"(프론트엔드|프론트|frontend|front[- ]?end)", re.IGNORECASE)),
    ("웹 풀스택", re.compile(r"(웹\s*풀스택|풀스택|full[- ]?stack)", re.IGNORECASE)),
    ("안드로이드", re.compile(r"(안드로이드|android)", re.IGNORECASE)),
    ("iOS", re.compile(r"(\bios\b|아이오에스|아이폰|iphone|ios\s*개발)", re.IGNORECASE)),
    ("크로스플랫폼", re.compile(r"(크로스\s*플랫폼|cross[- ]?platform|flutter|react\s*native|\brn\b|xamarin)", re.IGNORECASE)),
    ("머신러닝/AI", re.compile(r"(머신러닝|기계학습|딥러닝|\bai\b|\bml\b|machine\s*learning|deep\s*learning|인공지능)", re.IGNORECASE)),
    ("데이터 엔지니어", re.compile(r"(데이터\s*엔지니어|data\s*engineer)", re.IGNORECASE)),
    ("데이터 분석가", re.compile(r"(데이터\s*분석가|data\s*analyst|데이터\s*애널리스트)", re.IGNORECASE)),
    ("데이터 사이언티스트", re.compile(r"(데이터\s*사이언티스트|data\s*scientist)", re.IGNORECASE)),
    ("DevOps", re.compile(r"(\bdevops\b|데브옵스|dev\s*ops)", re.IGNORECASE)),
    ("시스템 엔지니어", re.compile(r"(시스템\s*엔지니어|system\s*engineer|\bsysadmin\b|시스어드민)", re.IGNORECASE)),
    ("클라우드 엔지니어", re.compile(r"(클라우드\s*엔지니어|cloud\s*engineer)", re.IGNORECASE)),
    ("DBA", re.compile(r"(\bdba\b|database\s*administrator|데이터베이스\s*(관리자|admin))", re.IGNORECASE)),
    ("SRE", re.compile(r"(\bsre\b|site\s*reliability\s*engineer)", re.IGNORECASE)),
    ("보안 엔지니어", re.compile(r"(보안\s*엔지니어|security\s*engineer|정보\s*보안|cyber\s*security)", re.IGNORECASE)),
    ("게임 클라이언트", re.compile(r"(게임\s*클라이언트|game\s*client)", re.IGNORECASE)),
    ("게임 서버", re.compile(r"(게임\s*서버|game\s*server)", re.IGNORECASE)),
    ("임베디드", re.compile(r"(임베디드|embedded)", re.IGNORECASE)),
    ("시스템 프로그래머", re.compile(r"(시스템\s*프로그래머|system\s*programmer|시스템\s*프로그래밍)", re.IGNORECASE)),
    ("QA 엔지니어", re.compile(r"(\bqa\b|qa\s*engineer|테스트\s*엔지니어|품질\s*보증)", re.IGNORECASE)),
    ("기술 PM", re.compile(r"(기술\s*pm|technical\s*pm|tech\s*pm)", re.IGNORECASE)),
    ("프로덕트 매니저", re.compile(r"(프로덕트\s*매니저|product\s*manager|프로덕트\s*pm|product\s*pm)", re.IGNORECASE)),
    ("UX/UI 디자이너", re.compile(r"(ux\s*/?\s*ui|ui\s*/?\s*ux|uxui|ux\s*디자이너|ui\s*디자이너)", re.IGNORECASE)),
    ("블록체인", re.compile(r"(블록체인|blockchain|\bweb3\b)", re.IGNORECASE)),
]

def normalize_position_label(raw: str | None) -> Optional[str]:
    if not raw:
        return None
    s = str(raw).strip()
    if not s:
        return None
    # exact match
    if s in POSITION_ORDER:
        return s
    sl = s.lower()
    if sl in ("all", "전체", "전부", "모두"):
        return "전체"
    # heuristic match
    for label, rx in _POSITION_PATTERNS:
        if rx.search(s):
            return label
    return None

def sanitize_positions(values: List[str] | None) -> List[str]:
    if not values:
        return []
    out: List[str] = []
    seen = set()
    for v in values:
        lab = normalize_position_label(v)
        if not lab or lab == "전체":
            continue
        if lab not in seen:
            out.append(lab)
            seen.add(lab)
    return out

def infer_positions_from_text(text: str | None) -> List[str]:
    if not text:
        return []
    t = str(text)
    found: List[str] = []
    seen = set()
    for label, rx in _POSITION_PATTERNS:
        if rx.search(t) and label not in seen:
            found.append(label)
            seen.add(label)
    return found

def build_position_patterns(positions_any: List[str] | None) -> List[str]:
    """Return LOWER(stack_item) LIKE ANY(text[]) patterns for the given positions.

    - '전체' 또는 빈 목록이면 [] 반환(필터 미적용)
    - 다중 포지션이면 키워드 union(OR)로 처리 (백엔드 JobPositionUtil.getKeywordsByPositions와 동일)
    """
    pos = sanitize_positions(positions_any)
    if not pos:
        return []
    kws: List[str] = []
    seen = set()
    for p in pos:
        for kw in POSITION_KEYWORDS.get(p, set()):
            k = str(kw).strip().lower()
            if not k or k in seen:
                continue
            kws.append(k)
            seen.add(k)
    return [f"%{k}%" for k in kws]
