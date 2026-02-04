from __future__ import annotations

import os
import uuid
from urllib.parse import urljoin
from typing import Any, Dict

from app.chatbot.schemas import ChatbotIntent, ChatbotParsedSpec
from app.chatbot.services.agent_core.stack_note import pick_stack_typo_note
from app.chatbot.services.agent_core.types import ChatbotState

def _frontend_base_url() -> str:
    """
    프론트 베이스 URL이 있으면 절대경로 링크를 만들고,
    없으면 상대경로(/jobs/{id})로만 반환.
    우선순위:
      1) CHATBOT_FRONTEND_BASE_URL
      2) FRONTEND_BASE_URL
    """
    return (os.getenv("CHATBOT_FRONTEND_BASE_URL") or os.getenv("FRONTEND_BASE_URL") or "").strip()


def _job_detail_path(job_id: int) -> str:
    return f"/jobs/{int(job_id)}"


def _job_detail_url(job_id: int) -> str:
    base = _frontend_base_url()
    path = _job_detail_path(job_id)
    if not base:
        return path
    base = base.rstrip("/") + "/"
    return urljoin(base, path.lstrip("/"))


def _md_escape_link_text(s: str) -> str:
    # 마크다운 링크 텍스트 최소 이스케이프
    return (s or "-").replace("[", "\\[").replace("]", "\\]")


def _employer_link(employer_name: str | None, job_id: int | None) -> str:
    name = _md_escape_link_text(str(employer_name or "-"))
    if job_id is None:
        return name
    return f"[{name}]({_job_detail_url(int(job_id))})"

async def render_state(state: ChatbotState) -> Dict[str, Any]:
    rid = state.get("request_id") or str(uuid.uuid4())
    parsed: ChatbotParsedSpec = state.get("parsed") or ChatbotParsedSpec(intent=ChatbotIntent.HELP, confidence=0.0)
    result = state.get("result") or {}

    if parsed.intent == ChatbotIntent.HELP:
        answer = (
            "지원 예시:\n"
            "- 오늘 공고 몇개 올라왔어?\n"
            "- 서울 강남구 연봉 4천 이상 공고 5개 랜덤\n"
            "- 1월 9일 뒤로 연봉 8천~9천 공고 몇개\n"
            "- 8월 백엔드 자바 최신 5개\n"
            "- 요즘 올라오는 공고에서 제일 많이 요구하는 스택\n"
            "- 경기도 요즘 평균 지원률/경쟁률\n"
            "- 경쟁률 120% 이하 공고 몇개\n"
            "- 신입/주니어/시니어/미들 공고 몇개?\n"
            "- 경력 3~5년 공고 몇개?\n"
            "- 산업분야 중 IT 관련 공고 몇개?\n"
            "- AI, 데이터 서비스 업종 공고 몇개?\n"
        )
        return {"answer": answer, "request_id": rid}

    start = result.get("start_date")
    end = result.get("end_date")

    typo_note = pick_stack_typo_note(result.get("stack_corrections") or [])

    def _maybe_prefix(body: str) -> str:
        return (typo_note + "\n" + body) if typo_note else body

    if parsed.intent == ChatbotIntent.COUNT_POSTINGS:
        cnt = int(result.get("count", 0))
        answer = _maybe_prefix(f"{start} ~ {end} 공고 수: {cnt}개")

    elif parsed.intent == ChatbotIntent.COMPETITION:
        postings = int(result.get("postings", 0))
        applications = int(result.get("applications", 0))
        avg = float(result.get("avg_apply_per_posting", 0.0))
        body = (
            f"{start} ~ {end} 경쟁률(지원수 기반):\n"
            f"- 공고 수: {postings}개\n"
            f"- 총 지원 수: {applications}건\n"
            f"- 공고 1개당 평균 지원 수: {avg:.2f}건"
        )
        answer = _maybe_prefix(body)

    elif parsed.intent == ChatbotIntent.RATE_STATS:
        postings = int(result.get("postings", 0))
        applications = int(result.get("applications", 0))
        cap = int(result.get("total_capacity", 0))
        avg_apply = float(result.get("avg_apply_per_posting", 0.0))
        apply_rate = float(result.get("apply_rate_weighted_pct", 0.0))
        comp_avg = float(result.get("competition_avg_pct", 0.0))

        body = (
            f"{start} ~ {end} 지원/경쟁 통계:\n"
            f"- 공고 수: {postings}개\n"
            f"- 총 지원 수: {applications}건\n"
            f"- 총 모집 인원: {cap}명\n"
            f"- 공고 1개당 평균 지원자 수: {avg_apply:.2f}명\n"
            f"- 지원률(총지원/총모집, 가중): {apply_rate:.2f}%\n"
            f"- 평균 경쟁률(공고별 %, 평균): {comp_avg:.2f}%"
        )
        answer = _maybe_prefix(body)


    elif parsed.intent in (ChatbotIntent.TOP_SALARY_POSTINGS, ChatbotIntent.BOTTOM_SALARY_POSTINGS):
        items = result.get("items") or []
        tag = "상위" if parsed.intent == ChatbotIntent.TOP_SALARY_POSTINGS else "하위"

        lines = [f"{start} ~ {end} 연봉 {tag} {len(items)}개:"]
        for i, it in enumerate(items, 1):
            link = _employer_link(it.get("employer_name"), it.get("job_id"))
            lines.append(
                f"{i}. {link} {it.get('title')}"
                f" / {it.get('location') or '-'}"
                f" / {it.get('salary_text') or '-'}"
                f" / {it.get('stack') or '-'}"
            )
            lines.append("")  # ✅ 항목 사이 빈 줄

        if lines and lines[-1] == "":
            lines.pop()

        answer = _maybe_prefix("\n".join(lines))

    elif parsed.intent == ChatbotIntent.LIST_POSTINGS:
        items = result.get("items") or []
        lim = int(result.get("limit", 5))
        rnd = bool(result.get("random"))

        lines = []
        # ✅ limit=1 & 결과 1개면 헤더를 생략하고 1줄 요약만 출력
        if not (lim == 1 and len(items) == 1):
            lines.append(f"{start} ~ {end} 공고 {len(items)}개:")

        for i, it in enumerate(items, 1):
            link = _employer_link(it.get("employer_name"), it.get("job_id"))
            lines.append(
                f"{i}. {link} {it.get('title')}"
                f" / {it.get('location') or '-'}"
                f" / {it.get('salary_text') or '-'}"
                f" / {it.get('stack') or '-'}"
            )
            lines.append("")

        if lines and lines[-1] == "":
            lines.pop()

        answer = _maybe_prefix("\n".join(lines))

    elif parsed.intent == ChatbotIntent.DETAIL_URLS:
        items = result.get("items") or []
        if not items:
            answer = "직전에 조회한 공고 목록이 없습니다. 먼저 공고 리스트를 조회한 뒤 다시 요청해주세요."
        else:
            lines = ["상세 페이지:"]
            for i, it in enumerate(items, 1):
                link = _employer_link(it.get("employer_name"), it.get("job_id"))
                title = it.get("title") or ""
                lines.append(f"{i}. {link} {title}".rstrip())
                lines.append("")  # ✅ 빈 줄

            if lines and lines[-1] == "":
                lines.pop()

            answer = _maybe_prefix("\n".join(lines))

    elif parsed.intent == ChatbotIntent.TOP_STACKS:
        items = result.get("items") or []
        lim = int(result.get("limit", 10))
        lines = [f"{start} ~ {end} TOP 스택 (상위 {lim}):"]
        for i, it in enumerate(items, 1):
            lines.append(f"{i}. {it['stack']} ({it['count']}회)")
        answer = _maybe_prefix("\n".join(lines))

    elif parsed.intent == ChatbotIntent.BOTTOM_STACKS:
        items = result.get("items") or []
        lim = int(result.get("limit", 10))
        lines = [f"{start} ~ {end} BOTTOM 스택 (하위 {lim}):"]
        for i, it in enumerate(items, 1):
            lines.append(f"{i}. {it['stack']} ({it['count']}회)")
        answer = _maybe_prefix("\n".join(lines))

    elif parsed.intent == ChatbotIntent.BUSIEST_WEEK:
        label = result.get("busiest_week_label") or "-"
        cnt = int(result.get("busiest_week_count", 0))
        ws = result.get("busiest_week_start_date")
        we = result.get("busiest_week_end_date")
        if ws and we:
            answer = (
                f"{start} ~ {end} 기간 기준으로, 공고가 제일 많이 올라온 주간은 {label}입니다. ({cnt}개)\n"
                f"- 주간 범위(KST): {ws} ~ {we}"
            )
        else:
            answer = f"{start} ~ {end} 기간 기준으로, 공고가 제일 많이 올라온 주간: {label} ({cnt}개)"

    elif parsed.intent == ChatbotIntent.BUSIEST_DAY:
        day = result.get("busiest_day") or "-"
        cnt = int(result.get("busiest_day_count", 0))
        answer = f"{start} ~ {end} 기간 기준으로, 공고가 제일 많이 올라온 날: {day} ({cnt}개)"

    elif parsed.intent in (ChatbotIntent.LOW_COMPETITION_POSTINGS, ChatbotIntent.HIGH_COMPETITION_POSTINGS):
        items = result.get("items") or []
        tag = "최저" if parsed.intent == ChatbotIntent.LOW_COMPETITION_POSTINGS else "최고"
        lines = [f"{start} ~ {end} 경쟁률 {tag} TOP {len(items)}:"]
        for i, it in enumerate(items, 1):
            pct = it.get("competition_pct")
            pct_txt = f"{float(pct):.2f}%" if pct is not None else "-"
            link = _employer_link(it.get("employer_name"), it.get("job_id"))
            lines.append(
                f"{i}. {link} {it.get('title')}"
                f" / {it.get('location') or '-'}"
                f" / {it.get('salary_text') or '-'}"
                f" / 경쟁률 {pct_txt}"
            )
            lines.append("")  # ✅ 빈 줄

            if lines and lines[-1] == "":
                lines.pop()

            answer = "\n".join(lines)

    elif parsed.intent in (ChatbotIntent.LOW_STACK_APPLY_RATE, ChatbotIntent.HIGH_STACK_APPLY_RATE):
        items = result.get("items") or []
        tag = "최저" if parsed.intent == ChatbotIntent.LOW_STACK_APPLY_RATE else "최고"
        lines = [f"{start} ~ {end} 스택 지원률 {tag} TOP {len(items)}:"]
        for i, it in enumerate(items, 1):
            pct = it.get("apply_rate_pct")
            pct_txt = f"{float(pct):.2f}%" if pct is not None else "-"
            lines.append(
                f"{i}. {it.get('stack')} / 지원률 {pct_txt} (지원 {it.get('apply_sum', 0)} / 모집 {it.get('cap_sum', 0)})"
            )
        answer = "\n".join(lines)

    elif parsed.intent in (ChatbotIntent.TOP_INDUSTRIES, ChatbotIntent.BOTTOM_INDUSTRIES):
        reason = result.get("reason")
        if reason:
            answer = str(reason)
        else:
            items = result.get("items") or []
            tag = "최고" if parsed.intent == ChatbotIntent.TOP_INDUSTRIES else "최저"
            lines = [f"{start} ~ {end} 산업 공고수 {tag} TOP {len(items)}:"]
            for i, it in enumerate(items, 1):
                lines.append(f"{i}. {it.get('industry')} ({it.get('count', 0)}개)")
            answer = "\n".join(lines)

    elif parsed.intent == ChatbotIntent.MOST_APPLICANTS_POSTINGS:
        items = result.get("items") or []
        lines = [f"{start} ~ {end} 지원자 수 최다 공고 (1개):"]
        for i, it in enumerate(items, 1):
            exp = it.get("required_experience")
            exp_str = f" ({exp}년 이상)" if exp is not None else ""
            ac = int(it.get("apply_count") or 0)
            lines.append(
                f"{i}. [{it.get('employer_name')}] {it.get('title')} / {it.get('location') or '-'} / {it.get('salary_text') or '-'}{exp_str}"
                f"{i}. [{it.get('employer_name')}] {it.get('title')} / {it.get('location') or '-'} / {it.get('salary_text') or '-'}{exp_str} / 지원자 수 {ac}명"
            )
        answer = "\n".join(lines)

    else:
        answer = "지원하지 않는 질문이다."

    if typo_note and not str(answer).startswith(typo_note):
        answer = typo_note + "\n" + str(answer)

    return {"answer": str(answer), "request_id": rid}
