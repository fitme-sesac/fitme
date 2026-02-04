from __future__ import annotations


def is_detail_url_request(msg: str) -> bool:
    """Detects whether the user is asking for detail page URLs/links.

    Examples:
    - "상세 페이지 url 줘"
    - "이거 링크 줘"
    - "상세페이지로 이동할 수 있게 주소 알려줘"
    - "연봉이 제일 높은 공고의 페이지로 이동하게 해줘"
    """

    if not msg:
        return False

    m = msg.strip()
    if not m:
        return False

    ml = m.lower()

    # URL/링크를 직접 언급
    has_url_word = (
        ("url" in ml)
        or ("link" in ml)
        or ("링크" in m)
        or ("주소" in m)
        or ("바로가기" in m)
    )

    # 상세/페이지를 지칭
    page_anchor_words = (
        "상세",
        "상세페이지",
        "상세 페이지",
        "페이지",
        "공고페이지",
        "공고 페이지",
    )
    has_page_anchor = any(w in m for w in page_anchor_words)

    # 이동/열기 의도
    move_words = (
        "이동",
        "가줘",
        "가게",
        "열어",
        "열어줘",
        "들어가",
        "접속",
        "넘어가",
        "바로가기",
    )
    has_move_intent = any(w in m for w in move_words)

    # 링크 제공 요청(= url 단어만 있고 '줘' 류만 있어도 True)
    give_words = ("줘", "달아", "알려", "보여", "보여줘", "제공", "보내", "적어", "찍어")
    has_give_intent = any(w in m for w in give_words)

    # 1) url/link/주소를 직접 언급하면 대부분 링크 요청으로 간주
    if has_url_word and (has_page_anchor or has_move_intent or has_give_intent):
        return True

    # 2) '페이지/상세' + '이동/열기' 조합이면 url 단어가 없어도 링크 요청으로 간주
    if has_page_anchor and has_move_intent:
        return True

    # 3) '공고' + '이동/열기'도 상세 페이지 이동으로 간주 (페이지 단어가 빠지는 경우)
    if ("공고" in m) and has_move_intent:
        return True

    return False


def is_count_request(msg: str) -> bool:
    """Detects whether the user is asking for a COUNT (number of postings), not a list/navigation.

    Examples:
    - "몇개야?"
    - "얼마나 있어?"
    - "많아?"
    - "어느정도 있어?"
    """
    if not msg:
        return False

    m = msg.replace(" ", "")
    # 핵심 카운트 패턴
    if any(k in m for k in ("몇개", "몇건", "건수", "개수")):
        return True
    if "얼마나" in msg:
        return True
    if ("어느정도" in m) or ("어느만큼" in m):
        return True
    # "많아?" 류
    if any(k in msg for k in ("많아", "많냐", "많은지", "많을까", "많음")):
        return True
    return False


def is_filtered_jobs_page_request(msg: str) -> bool:
    """Detects whether the user wants to open the Jobs page with filters applied.

    Heuristic:
    - Has an 'open/list/show' verb (알려줘/보여줘/목록/리스트/이동/페이지)
    - Mentions jobs/postings (공고/채용)
    - Not a count request
    """
    if not msg:
        return False
    if is_count_request(msg):
        return False

    ml = msg.lower()

    # navigation verbs
    has_nav = any(k in msg for k in ("알려줘", "보여줘", "목록", "리스트", "이동", "페이지", "열어", "바로가기", "띄워"))
    if not has_nav:
        return False

    # should be about job postings
    if not any(k in msg for k in ("공고", "채용", "채용공고")):
        return False

    # filter hints (at least one)
    has_filter_hint = any(
        k in msg
        for k in (
            "연봉", "급여", "경력", "지역", "근무지", "스택", "요구", "기술", "포지션", "직군", "직업군",
            "산업", "업종", "분야", "서비스분야", "서비스 분야",
        )
    ) or any(k in ml for k in ("stack", "backend", "front", "fullstack", "python", "java", "react", "spring", "ai"))

    return has_filter_hint

