# app/employer_chatbot/services/render.py
"""
응답 렌더링 모듈
- DB 결과를 사용자 친화적 텍스트로 변환
"""
from __future__ import annotations

import uuid
from typing import Any, Dict

from app.employer_chatbot.schemas import EmployerChatbotIntent, EmployerChatbotParsedSpec


def _format_number(n: int) -> str:
    """숫자 포맷팅 (천 단위 콤마)"""
    return f"{n:,}"


def _format_percent(pct: float | None, decimals: int = 1) -> str:
    """퍼센트 포맷팅"""
    if pct is None:
        return "-"
    return f"{pct:.{decimals}f}%"


def _status_korean(status: str) -> str:
    """지원 상태 한글 변환"""
    mapping = {
        "PENDING": "대기중",
        "REVIEWED": "검토완료",
        "SHORTLISTED": "서류통과",
        "INTERVIEW": "면접진행",
        "REJECTED": "불합격",
        "HIRED": "합격",
    }
    return mapping.get(status.upper(), status)


def _filters_summary(result: Dict[str, Any]) -> str:
    """필터 요약 텍스트 생성"""
    parts = []
    
    if result.get("job_posting_ids"):
        parts.append(f"공고ID: {result['job_posting_ids']}")
    if result.get("job_title_keywords"):
        parts.append(f"공고키워드: {', '.join(result['job_title_keywords'])}")
    if result.get("application_status_any"):
        statuses = [_status_korean(s) for s in result['application_status_any']]
        parts.append(f"상태: {', '.join(statuses)}")
    if result.get("min_experience_years") is not None or result.get("max_experience_years") is not None:
        min_exp = result.get("min_experience_years", 0)
        max_exp = result.get("max_experience_years")
        if max_exp is not None:
            parts.append(f"경력: {min_exp}~{max_exp}년")
        else:
            parts.append(f"경력: {min_exp}년 이상")
    
    return f"📋 {' | '.join(parts)}" if parts else ""


async def render_response(
    parsed: EmployerChatbotParsedSpec,
    result: Dict[str, Any],
    request_id: str | None = None,
) -> Dict[str, Any]:
    """
    파싱 결과와 DB 결과를 사용자 응답으로 렌더링
    """
    rid = request_id or str(uuid.uuid4())
    
    # HELP 인텐트
    if parsed.intent == EmployerChatbotIntent.HELP:
        answer = _render_help()
        return {"answer": answer, "request_id": rid}
    
    start = result.get("start_date", "-")
    end = result.get("end_date", "-")
    filters_text = _filters_summary(result)
    
    # 각 인텐트별 렌더링
    if parsed.intent == EmployerChatbotIntent.COUNT_APPLICATIONS:
        answer = _render_count_applications(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.LIST_APPLICATIONS:
        answer = _render_list_applications(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.APPLICATION_STATS:
        answer = _render_application_stats(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.APPLICANT_TREND:
        answer = _render_applicant_trend(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.POSTING_PERFORMANCE:
        answer = _render_posting_performance(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.COMPARE_POSTINGS:
        answer = _render_compare_postings(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.TOP_SKILLS:
        answer = _render_top_skills(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.APPLICANT_PROFILE:
        answer = _render_applicant_profile(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.CONVERSION_RATE:
        answer = _render_conversion_rate(start, end, result, filters_text)
    
    elif parsed.intent == EmployerChatbotIntent.URGENT_ACTIONS:
        answer = _render_urgent_actions(result)
    
    elif parsed.intent == EmployerChatbotIntent.PENDING_REVIEW:
        answer = _render_pending_review(start, end, result, filters_text)
    
    else:
        answer = "⚠️ 지원하지 않는 질문입니다. 다시 시도해주세요."
    
    return {"answer": answer, "request_id": rid}


# =============================================================================
# 개별 렌더링 함수
# =============================================================================

def _render_help() -> str:
    """도움말 렌더링"""
    return """📌 **기업 채용 AI 어시스턴트 사용법**

💼 **지원자 조회**
• "오늘 지원자 몇명이야?"
• "이번달 지원자 목록 보여줘"
• "서류 통과한 지원자 누구야?"

📊 **통계 & 분석**
• "지원 현황 분석해줘"
• "이번주 지원자 추이는?"
• "채용 전환율 어때?"

🎯 **공고 성과**
• "우리 공고 성과 분석해줘"
• "어떤 공고가 가장 인기있어?"
• "백엔드와 프론트엔드 공고 비교해줘"

👤 **인재 분석**
• "지원자들이 많이 보유한 스킬은?"
• "지원자 평균 경력은?"

⚡ **빠른 확인**
• "검토 안한 지원자 있어?"
• "긴급하게 처리할 건 있어?"

💡 **필터 예시**
• "백엔드 공고 지원자만"
• "경력 3년 이상 지원자"
• "서울 지역 지원자"
"""


def _render_count_applications(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """지원자 수 렌더링"""
    cnt = int(result.get("count", 0))
    
    lines = [
        f"📊 **{start} ~ {end} 지원자 수**",
        "",
        f"✅ 총 **{_format_number(cnt)}명**이 지원했습니다.",
    ]
    
    if filters_text:
        lines.append("")
        lines.append(filters_text)
    
    return "\n".join(lines)


def _render_list_applications(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """지원자 목록 렌더링"""
    items = result.get("items") or []
    lim = int(result.get("limit", 10))
    rnd = bool(result.get("random"))
    
    sort_type = "🎲 랜덤" if rnd else "📅 최신순"
    
    lines = [
        f"📋 **{start} ~ {end} 지원자 목록** ({sort_type})",
        f"총 {len(items)}명 표시 (최대 {lim}명)",
        "",
    ]
    
    if filters_text:
        lines.append(filters_text)
        lines.append("")
    
    for i, it in enumerate(items, 1):
        status = _status_korean(it.get('status', '-'))
        lines.append(
            f"**{i}. {it.get('applicant_name', '-')}** ({it.get('applicant_email', '-')})\n"
            f"   📌 공고: {it.get('job_title', '-')}\n"
            f"   📍 상태: {status} | 📆 지원일: {it.get('created_at', '-')[:10] if it.get('created_at') else '-'}"
        )
        lines.append("")
    
    if not items:
        lines.append("❌ 조건에 맞는 지원자가 없습니다.")
    
    return "\n".join(lines)


def _render_application_stats(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """지원 통계 렌더링"""
    total = int(result.get("total_applications", 0))
    status_dist = result.get("status_distribution") or {}
    by_posting = result.get("by_posting") or []
    
    lines = [
        f"📊 **{start} ~ {end} 지원 통계**",
        "",
        f"✅ 총 지원자 수: **{_format_number(total)}명**",
        "",
        "📈 **상태별 분포**",
    ]
    
    if status_dist:
        for status, cnt in status_dist.items():
            status_kr = _status_korean(status)
            pct = (cnt / total * 100) if total > 0 else 0
            lines.append(f"  • {status_kr}: {_format_number(cnt)}명 ({_format_percent(pct)})")
    else:
        lines.append("  • 데이터 없음")
    
    if by_posting:
        lines.append("")
        lines.append("🏆 **공고별 지원자 수 TOP**")
        for p in by_posting[:5]:
            lines.append(f"  • [{p['job_id']}] {p['title']}: {_format_number(p['count'])}명")
    
    if filters_text:
        lines.append("")
        lines.append(filters_text)
    
    return "\n".join(lines)


def _render_applicant_trend(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """지원자 추이 렌더링"""
    trend_data = result.get("trend") or []
    time_unit = result.get("time_unit", "day")
    
    unit_kr = {"day": "일별", "week": "주별", "month": "월별"}.get(time_unit, time_unit)
    
    lines = [
        f"📈 **{start} ~ {end} 지원자 추이** ({unit_kr})",
        "",
    ]
    
    if trend_data:
        for item in trend_data[:10]:
            period = item.get("period", "-")
            count = int(item.get("count", 0))
            # 간단한 막대 그래프
            bar = "▓" * min(count // 2, 20)
            lines.append(f"  {period}: {bar} {_format_number(count)}명")
    else:
        lines.append("❌ 추이 데이터가 없습니다.")
    
    if filters_text:
        lines.append("")
        lines.append(filters_text)
    
    return "\n".join(lines)


def _render_posting_performance(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """공고 성과 렌더링"""
    items = result.get("items") or []
    
    lines = [
        f"🎯 **{start} ~ {end} 공고 성과 분석**",
        f"총 {len(items)}개 공고",
        "",
    ]
    
    if filters_text:
        lines.append(filters_text)
        lines.append("")
    
    for i, it in enumerate(items, 1):
        comp_str = _format_percent(it.get('competition_pct'))
        capacity = it.get('recruitment_capacity') or '-'
        
        lines.append(
            f"**{i}. [{it['job_id']}] {it['title']}**\n"
            f"   📍 상태: {it['status']}\n"
            f"   👥 지원: {_format_number(it['apply_count'])}명 / 모집: {capacity}명\n"
            f"   📊 경쟁률: {comp_str}"
        )
        lines.append("")
    
    if not items:
        lines.append("❌ 분석할 공고가 없습니다.")
    
    return "\n".join(lines)


def _render_compare_postings(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """공고 비교 렌더링"""
    items = result.get("items") or []
    
    lines = [
        f"⚖️ **{start} ~ {end} 공고 비교 분석**",
        "",
    ]
    
    if len(items) >= 2:
        for i, it in enumerate(items[:5], 1):
            lines.append(
                f"**{i}. {it.get('title', '-')}**\n"
                f"   • 지원자: {_format_number(it.get('apply_count', 0))}명\n"
                f"   • 경쟁률: {_format_percent(it.get('competition_pct'))}"
            )
            lines.append("")
    else:
        lines.append("❌ 비교할 공고가 2개 이상 필요합니다.")
    
    if filters_text:
        lines.append(filters_text)
    
    return "\n".join(lines)


def _render_top_skills(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """TOP 스킬 렌더링"""
    items = result.get("items") or []
    lim = int(result.get("limit", 10))
    
    lines = [
        f"🛠️ **{start} ~ {end} 지원자 보유 스킬 TOP {lim}**",
        "",
    ]
    
    if items:
        for i, it in enumerate(items, 1):
            skill = it.get('skill', '-')
            count = int(it.get('count', 0))
            # 메달 아이콘
            medal = {1: "🥇", 2: "🥈", 3: "🥉"}.get(i, f"{i}.")
            lines.append(f"{medal} **{skill}** - {_format_number(count)}명 보유")
    else:
        lines.append("❌ 스킬 데이터가 없습니다.")
        if result.get("reason"):
            lines.append(f"   (사유: {result['reason']})")
    
    if filters_text:
        lines.append("")
        lines.append(filters_text)
    
    return "\n".join(lines)


def _render_applicant_profile(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """지원자 프로필 요약 렌더링"""
    total = int(result.get("total", 0))
    avg_exp = result.get("avg_experience")
    exp_dist = result.get("experience_distribution") or {}
    
    lines = [
        f"👥 **{start} ~ {end} 지원자 프로필 분석**",
        "",
        f"📊 총 {_format_number(total)}명 지원자 분석 결과",
        "",
    ]
    
    if avg_exp is not None:
        lines.append(f"📈 평균 경력: **{avg_exp:.1f}년**")
    
    if exp_dist:
        lines.append("")
        lines.append("👔 **경력 분포**")
        for exp_range, cnt in exp_dist.items():
            pct = (cnt / total * 100) if total > 0 else 0
            lines.append(f"  • {exp_range}: {_format_number(cnt)}명 ({_format_percent(pct)})")
    
    if filters_text:
        lines.append("")
        lines.append(filters_text)
    
    return "\n".join(lines)


def _render_conversion_rate(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """전환율 렌더링"""
    stages = result.get("stages") or {}
    overall_rate = result.get("overall_conversion_rate")
    
    lines = [
        f"📊 **{start} ~ {end} 채용 퍼널 전환율**",
        "",
    ]
    
    # 퍼널 시각화
    funnel_stages = [
        ("지원", "PENDING"),
        ("검토완료", "REVIEWED"),
        ("서류통과", "SHORTLISTED"),
        ("면접", "INTERVIEW"),
        ("합격", "HIRED"),
    ]
    
    prev_count = None
    for stage_name, stage_key in funnel_stages:
        count = stages.get(stage_key, 0)
        if prev_count is not None and prev_count > 0:
            rate = (count / prev_count) * 100
            lines.append(f"  ↓ 전환율: {_format_percent(rate)}")
        lines.append(f"📍 **{stage_name}**: {_format_number(count)}명")
        prev_count = count if count > 0 else prev_count
    
    if overall_rate is not None:
        lines.append("")
        lines.append(f"🎯 **최종 합격률**: {_format_percent(overall_rate)}")
    
    if filters_text:
        lines.append("")
        lines.append(filters_text)
    
    return "\n".join(lines)


def _render_urgent_actions(result: Dict[str, Any]) -> str:
    """긴급 조치 필요 항목 렌더링"""
    urgent_items = result.get("items") or []
    
    lines = [
        "⚡ **긴급 처리 필요 항목**",
        "",
    ]
    
    if urgent_items:
        for item in urgent_items:
            item_type = item.get("type", "기타")
            count = int(item.get("count", 0))
            desc = item.get("description", "")
            lines.append(f"🔴 **{item_type}**: {_format_number(count)}건")
            if desc:
                lines.append(f"   {desc}")
            lines.append("")
    else:
        lines.append("✅ 현재 긴급 처리가 필요한 항목이 없습니다!")
    
    return "\n".join(lines)


def _render_pending_review(start: str, end: str, result: Dict[str, Any], filters_text: str) -> str:
    """검토 대기 렌더링"""
    count = int(result.get("count", 0))
    items = result.get("items") or []
    
    lines = [
        f"📋 **검토 대기 중인 지원자**",
        "",
        f"⏳ 총 **{_format_number(count)}명**이 검토를 기다리고 있습니다.",
        "",
    ]
    
    if items:
        lines.append("**최근 대기자:**")
        for i, it in enumerate(items[:5], 1):
            days_waiting = it.get("days_waiting", 0)
            lines.append(
                f"{i}. {it.get('applicant_name', '-')} - "
                f"{it.get('job_title', '-')} (대기 {days_waiting}일)"
            )
    
    if count > 5:
        lines.append(f"\n... 외 {count - 5}명")
    
    if filters_text:
        lines.append("")
        lines.append(filters_text)
    
    return "\n".join(lines)
