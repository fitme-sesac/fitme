from __future__ import annotations

def is_detail_url_request(msg: str) -> bool:
    """Detects whether the user is asking for detail page URLs/links.

    Examples:
    - "상세 페이지 url 줘"
    - "이거 링크 줘"
    - "상세페이지로 이동할 수 있게 주소 알려줘"
    """
    if not msg:
        return False

    ml = msg.lower()

    has_url_word = ("url" in ml) or ("link" in ml) or ("링크" in msg) or ("주소" in msg)
    if not has_url_word:
        return False

    has_page_word = any(k in msg for k in ("상세", "상세페이지", "상세 페이지", "페이지", "이동", "바로가기", "열어", "들어가"))
    return has_page_word
