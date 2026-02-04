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
