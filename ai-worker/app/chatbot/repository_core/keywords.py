# app/chatbot/repository_core/keywords.py
from __future__ import annotations
from .schema import _column_exists
from typing import Any, Dict, List, Optional, Tuple

def _keyword_group_predicate(alias: Optional[str] = None) -> List[str]:
    prefix = f"{alias}." if alias else ""
    preds = [
        f"{prefix}title ILIKE %(kw)s",
        f"{prefix}description ILIKE %(kw)s",
    ]

    if _column_exists("job_posting", "stack"):
        preds.insert(
            0,
            f"""
            EXISTS (
              SELECT 1
              FROM unnest(coalesce({prefix}stack, ARRAY[]::text[])) AS st(item)
              CROSS JOIN LATERAL regexp_split_to_table(coalesce(st.item, ''), '\\s*[,/|]+\\s*') AS tok
              WHERE lower(btrim(tok)) ILIKE lower(%(kw)s)
            )
            """.strip(),
        )
    return preds

def _build_keywords_where_and_params_v2(
        keywords_all_groups: List[List[str]],
        keywords_any_groups: List[List[str]],
        alias: Optional[str] = None,
        param_offset: int = 0,
) -> Tuple[List[str], Dict[str, Any]]:
    where_parts: List[str] = []
    params: Dict[str, Any] = {}
    preds = _keyword_group_predicate(alias=alias)
    idx = param_offset

    def _group_sql_for_variants(variants: List[str]) -> str:
        nonlocal idx
        variant_clauses: List[str] = []
        for v in variants:
            key = f"kw{idx}"
            params[key] = f"%{v}%"
            field_or = "(" + " OR ".join(p.replace("%(kw)s", f"%({key})s") for p in preds) + ")"
            variant_clauses.append(field_or)
            idx += 1
        return "(" + " OR ".join(variant_clauses) + ")"

    for variants in keywords_all_groups:
        variants = [v for v in variants if v]
        if not variants:
            continue
        where_parts.append(_group_sql_for_variants(variants))

    if keywords_any_groups:
        or_groups: List[str] = []
        for variants in keywords_any_groups:
            variants = [v for v in variants if v]
            if not variants:
                continue
            or_groups.append(_group_sql_for_variants(variants))
        if or_groups:
            where_parts.append("(" + " OR ".join(or_groups) + ")")

    return where_parts, params

__all__ = [
    "_keyword_group_predicate",
    "_build_keywords_where_and_params_v2",
]
