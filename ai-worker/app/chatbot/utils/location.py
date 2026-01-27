# app/chatbot/utils/location.py
from __future__ import annotations

import re
from typing import Any, Dict, List, Optional, Tuple

# ✅ 추가: 조사/어미 제거
_JOSA_RE = re.compile(r"(은|는|이|가|을|를|에|에서|으로|로|도|만|야|요|죠|까)$")

REGION_SYNONYMS: Dict[str, str] = {
    "서울": "SEOUL", "서울시": "SEOUL", "서울특별시": "SEOUL", "seoul": "SEOUL",
    "부산": "BUSAN", "부산시": "BUSAN", "부산광역시": "BUSAN", "busan": "BUSAN",
    "대구": "DAEGU", "대구시": "DAEGU", "대구광역시": "DAEGU", "daegu": "DAEGU",
    "인천": "INCHEON", "인천시": "INCHEON", "인천광역시": "INCHEON", "incheon": "INCHEON",
    "광주": "GWANGJU", "광주시": "GWANGJU", "광주광역시": "GWANGJU", "gwangju": "GWANGJU",
    "대전": "DAEJEON", "대전시": "DAEJEON", "대전광역시": "DAEJEON", "daejeon": "DAEJEON",
    "울산": "ULSAN", "울산시": "ULSAN", "울산광역시": "ULSAN", "ulsan": "ULSAN",
    "세종": "SEJONG", "세종시": "SEJONG", "세종특별자치시": "SEJONG", "sejong": "SEJONG",
    "경기": "GYEONGGI", "경기도": "GYEONGGI", "gyeonggi": "GYEONGGI",
    "강원": "GANGWON", "강원도": "GANGWON", "강원특별자치도": "GANGWON", "gangwon": "GANGWON",
    "충북": "CHUNGBUK", "충청북도": "CHUNGBUK", "chungbuk": "CHUNGBUK",
    "충남": "CHUNGNAM", "충청남도": "CHUNGNAM", "chungnam": "CHUNGNAM",
    "전북": "JEONBUK", "전라북도": "JEONBUK", "전북특별자치도": "JEONBUK", "jeonbuk": "JEONBUK",
    "전남": "JEONNAM", "전라남도": "JEONNAM", "jeonnam": "JEONNAM",
    "경북": "GYEONGBUK", "경상북도": "GYEONGBUK", "gyeongbuk": "GYEONGBUK",
    "경남": "GYEONGNAM", "경상남도": "GYEONGNAM", "gyeongnam": "GYEONGNAM",
    "제주": "JEJU", "제주도": "JEJU", "제주특별자치도": "JEJU", "jeju": "JEJU",
}

REGION_LOCATION_PATTERNS: Dict[str, List[str]] = {
    "SEOUL":    ["%서울%", "%서울시%", "%서울특별시%", "%seoul%"],
    "BUSAN":    ["%부산%", "%부산시%", "%부산광역시%", "%busan%"],
    "DAEGU":    ["%대구%", "%대구시%", "%대구광역시%", "%daegu%"],
    "INCHEON":  ["%인천%", "%인천시%", "%인천광역시%", "%incheon%"],
    "GWANGJU":  ["%광주%", "%광주시%", "%광주광역시%", "%gwangju%"],
    "DAEJEON":  ["%대전%", "%대전시%", "%대전광역시%", "%daejeon%"],
    "ULSAN":    ["%울산%", "%울산시%", "%울산광역시%", "%ulsan%"],
    "SEJONG":   ["%세종%", "%세종시%", "%세종특별자치시%", "%sejong%"],
    "GYEONGGI": ["%경기%", "%경기도%", "%gyeonggi%"],
    "GANGWON":  ["%강원%", "%강원도%", "%강원특별자치도%", "%gangwon%"],
    "CHUNGBUK": ["%충북%", "%충청북도%", "%chungbuk%"],
    "CHUNGNAM": ["%충남%", "%충청남도%", "%chungnam%"],
    "JEONBUK":  ["%전북%", "%전라북도%", "%전북특별자치도%", "%jeonbuk%"],
    "JEONNAM":  ["%전남%", "%전라남도%", "%jeonnam%"],
    "GYEONGBUK":["%경북%", "%경상북도%", "%gyeongbuk%"],
    "GYEONGNAM":["%경남%", "%경상남도%", "%gyeongnam%"],
    "JEJU":     ["%제주%", "%제주도%", "%제주특별자치도%", "%jeju%"],
}

REGION_ALLOWED_SUFFIXES: Dict[str, Tuple[str, ...]] = {
    "SEOUL": ("구",),
    "BUSAN": ("구", "군"),
    "DAEGU": ("구", "군"),
    "INCHEON": ("구", "군"),
    "GWANGJU": ("구", "군"),
    "DAEJEON": ("구", "군"),
    "ULSAN": ("구", "군"),
    "SEJONG": tuple(),  # 필요하면 읍/면/동 확장
    "GYEONGGI": ("시", "군", "구"),
    "GANGWON": ("시", "군", "구"),
    "CHUNGBUK": ("시", "군", "구"),
    "CHUNGNAM": ("시", "군", "구"),
    "JEONBUK": ("시", "군", "구"),
    "JEONNAM": ("시", "군", "구"),
    "GYEONGBUK": ("시", "군", "구"),
    "GYEONGNAM": ("시", "군", "구"),
    "JEJU": ("시",),
}

ADMIN_AREA_EXPLICIT_RE = re.compile(r"([가-힣]{2,10})(구|시|군)\b")
HANGUL_WORD_RE = re.compile(r"[가-힣]{2,10}")

STOPWORDS = {
    "공고","채용","개","건","알려줘","최신","랜덤","요즘","오늘","이번","올해","월",
    "연봉","이상","미만","직군","직업군","스택","요구","요구하는","관련","중","만","정도","몇",
    "개발","개발자","업무","포지션","포지션명",
    "수","수는","건수","갯수","개수",
    "지역", "몇개", "몇개야"
}


def normalize_region_token(token: str) -> Optional[str]:
    k = token.strip().lower()
    return REGION_SYNONYMS.get(k)


def infer_regions_from_text(text: str) -> List[str]:
    if not text:
        return []
    t = text.strip()
    tl = t.lower()

    hits: List[str] = []
    for k, v in REGION_SYNONYMS.items():
        if k.isascii():
            if k in tl:
                hits.append(v)
        else:
            if k in t:
                hits.append(v)

    out: List[str] = []
    for x in hits:
        if x not in out:
            out.append(x)
    return out


def infer_admin_areas_from_text(text: str, regions_any: List[str]) -> List[str]:
    if not text:
        return []
    t = text.strip()

    # 1) explicit: 강남구/수원시/기장군...
    explicit = [m.group(1) + m.group(2) for m in ADMIN_AREA_EXPLICIT_RE.finditer(t)]
    explicit = list(dict.fromkeys(explicit))
    if explicit:
        return explicit

    # 2) heuristic: take last non-stopword hangul token and attach allowed suffixes
    words = HANGUL_WORD_RE.findall(t)

    # ✅ 조사 제거 + 공백 제거
    words2: List[str] = []
    for w in words:
        w2 = _JOSA_RE.sub("", w)
        if w2:
            words2.append(w2)

    region_keys = {k for k in REGION_SYNONYMS.keys() if not k.isascii()}
    # ✅ words2 사용 + "지역" 포함 토큰 제외 권장
    candidates = [
        w for w in words2
        if (w not in STOPWORDS and w not in region_keys and "지역" not in w)
    ]
    if not candidates:
        return []

    base = candidates[-1]
    if len(base) < 2:
        return []

    base = candidates[-1]

    # ✅ 조사 제거 결과가 1글자면 행정구역 후보로 쓰지 않음 ("수" 같은 것)
    if len(base) < 2:
        return []

    suffixes = set()
    for r in regions_any or []:
        suffixes.update(REGION_ALLOWED_SUFFIXES.get(r, ()))
    if not suffixes:
        suffixes = {"시", "군", "구"}

    out: List[str] = []
    for sfx in ("구", "시", "군"):
        if sfx in suffixes:
            out.append(base + sfx)
    out.append(base)  # suffix-less fallback

    uniq: List[str] = []
    for x in out:
        if x not in uniq:
            uniq.append(x)
    return uniq


def apply_location_filters(
        where: List[str],
        params: Dict[str, Any],
        regions_any: Optional[List[str]] = None,
        admin_areas_any: Optional[List[str]] = None,
        location_col: str = "location",
) -> None:
    loc_expr = f"COALESCE({location_col}, '')"

    # (광역 OR)
    if regions_any:
        ors: List[str] = []
        idx = 0
        for r in regions_any:
            for pat in REGION_LOCATION_PATTERNS.get(r, []):
                key = f"reg{idx}"
                ors.append(f"{loc_expr} ILIKE %({key})s")
                params[key] = pat
                idx += 1
        if ors:
            where.append("(" + " OR ".join(ors) + ")")

    # (시군구 OR)
    if admin_areas_any:
        ors: List[str] = []
        for i, a in enumerate(admin_areas_any):
            key = f"adm{i}"
            ors.append(f"{loc_expr} ILIKE %({key})s")
            params[key] = f"%{a}%"
        if ors:
            where.append("(" + " OR ".join(ors) + ")")
