from __future__ import annotations

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional

from app.resumes.schemas import ResumeRequest
from app.resumes.services.summary import summary_service
from app.resumes.services.vector import vector_service

router = APIRouter(prefix="/api/ai/resume", tags=["legacy-compat"])


class Suggestion(BaseModel):
    section: str = Field(description="project/career/content/skills 등")
    type: str = Field(description="add/modify/remove/enhance")
    original: Optional[str] = None
    suggested: str
    reason: str
    priority: int = 3


class MatchAnalysis(BaseModel):
    match_score: Optional[float] = None
    matched_skills: List[str] = Field(default_factory=list)
    missing_skills: List[str] = Field(default_factory=list)
    highlight_points: List[str] = Field(default_factory=list)
    improvement_areas: List[str] = Field(default_factory=list)


class AiResumeResponse(BaseModel):
    resume_id: int
    status: str
    summary: str = ""
    suggestions: List[Suggestion] = Field(default_factory=list)
    improved_content: Optional[str] = None
    skill_recommendations: List[str] = Field(default_factory=list)
    match_analysis: Optional[MatchAnalysis] = None
    error_message: Optional[str] = None


def _unique_nonempty(items: List[Optional[str]]) -> List[str]:
    seen = set()
    out: List[str] = []
    for it in items or []:
        if not it:
            continue
        s = str(it).strip()
        if not s:
            continue
        key = s.lower()
        if key in seen:
            continue
        seen.add(key)
        out.append(s)
    return out


def _cosine_similarity(a: List[float], b: List[float]) -> float:
    if not a or not b or len(a) != len(b):
        return 0.0
    dot = 0.0
    na = 0.0
    nb = 0.0
    for x, y in zip(a, b):
        dot += x * y
        na += x * x
        nb += y * y
    if na <= 0.0 or nb <= 0.0:
        return 0.0
    return dot / ((na ** 0.5) * (nb ** 0.5))


def _build_basic_suggestions(req: ResumeRequest) -> List[Suggestion]:
    s: List[Suggestion] = []
    # 매우 가벼운 휴리스틱: 데이터 유무/길이로만 제안 생성 (LLM 추가 호출 없음)
    if not req.projects:
        s.append(Suggestion(
            section="project",
            type="add",
            suggested="대표 프로젝트 1~3개를 추가하고, 역할/성과(수치)를 명시하세요.",
            reason="프로젝트는 직무 적합성을 가장 빠르게 증명하는 근거입니다.",
            priority=1,
        ))
    if not req.careers:
        s.append(Suggestion(
            section="career",
            type="add",
            suggested="경력/인턴/대외활동이 있다면 기간·역할·성과 중심으로 추가하세요.",
            reason="실무 경험은 채용공고와의 매칭 품질을 크게 올립니다.",
            priority=2,
        ))
    if req.content and len(req.content.strip()) < 200:
        s.append(Suggestion(
            section="content",
            type="enhance",
            original=req.content.strip(),
            suggested="자기소개/경험 서술을 STAR(상황-과제-행동-결과) 구조로 3~5문단 확장하세요.",
            reason="짧은 본문은 역량 근거가 부족해 보일 수 있습니다.",
            priority=2,
        ))
    if req.basic_info and (not req.basic_info.re_stack or len(req.basic_info.re_stack) < 3):
        s.append(Suggestion(
            section="skills",
            type="enhance",
            suggested="주요 기술스택을 5개 이상(언어/프레임워크/DB/인프라)로 구체화하세요.",
            reason="스택 정보가 부족하면 추천/검색/매칭 정확도가 떨어집니다.",
            priority=3,
        ))
    return s


@router.post("/analyze", response_model=AiResumeResponse)
async def analyze_resume(request: ResumeRequest):
    """
    [LEGACY COMPAT] Java 백엔드 AiResumeService가 호출하는 경로(/api/ai/resume/analyze)를 제공합니다.

    - 최신 워커의 /resumes/generate-summary 를 내부적으로 사용해 요약을 생성합니다.
    - Java가 최소로 사용하는 필드(summary, skill_recommendations, status)를 보장합니다.
    - target_job이 있으면 embedding 유사도로 match_score를 계산해 match_analysis를 채웁니다.
    """
    try:
        summary_result, eval_info = await summary_service.generate_summary(
            request.model_dump(),
            request.basic_info.field,
            request.summary_type,
            resume_id=request.resume_id,
        )

        if isinstance(summary_result, str):
            display_summary = summary_result
        else:
            meta_title = request.basic_info.title or ""
            meta_stack = ", ".join(request.basic_info.re_stack) if request.basic_info.re_stack else ""
            display_summary = summary_result.to_formatted_string(include_reasoning=request.include_reasoning)
            # embedding은 여기서 필요시만 생성. (generate_summary와의 중복 호출 방지)

        skills = _unique_nonempty(list(getattr(request.basic_info, "re_stack", []) or []))

        resp = AiResumeResponse(
            resume_id=int(request.resume_id),
            status="SUCCESS",
            summary=display_summary,
            suggestions=_build_basic_suggestions(request),
            improved_content=request.content,
            skill_recommendations=skills,
        )

        # job-match 분석(선택): 임베딩 기반 유사도만 계산
        if request.target_job and (request.target_job.title or request.target_job.description):
            job_text = f"{request.target_job.title or ''}\n{request.target_job.description or ''}".strip()
            if job_text:
                resume_vec = await vector_service.generate_vector(display_summary)
                job_vec = await vector_service.generate_vector(job_text)
                sim = _cosine_similarity(resume_vec, job_vec)
                score = max(0.0, min(100.0, sim * 100.0))

                # 단순 스킬 매칭(문자열 포함 + 교집합)
                job_lower = job_text.lower()
                matched = [s for s in skills if s.lower() in job_lower]
                missing = [s for s in skills if s.lower() not in job_lower]

                resp.match_analysis = MatchAnalysis(
                    match_score=round(score, 2),
                    matched_skills=matched,
                    missing_skills=missing,
                    highlight_points=[
                        "요약에서 가장 강한 성과/지표를 첫 3줄에 배치",
                        "공고 요구 스택과 정확히 동일한 키워드로 표기",
                    ],
                    improvement_areas=[
                        "프로젝트 성과를 수치로 보강",
                        "공고 핵심 요구사항과 직접 연결되는 경험을 상단에 배치",
                    ],
                )

        return resp

    except Exception as e:
        # Java가 파싱할 수 있도록 스키마는 유지하되 FAILED로 반환
        return AiResumeResponse(
            resume_id=int(getattr(request, "resume_id", 0) or 0),
            status="FAILED",
            summary="",
            skill_recommendations=[],
            error_message=str(e),
        )
