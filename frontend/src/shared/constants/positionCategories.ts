/**
 * 채용공고 stack 기반 포지션 카테고리 (백엔드 JobPositionUtil과 동일한 기준)
 * - 프론트엔드 기술 위주 → "프론트엔드"
 * - 백엔드 기술 위주 → "백엔드"
 * - 둘 다 포함 → "풀스택"
 */

/** API/백엔드에서 사용하는 포지션 값 (3종만 사용) */
export const POSITION_CATEGORIES = ["프론트엔드", "백엔드", "풀스택"] as const;

export type PositionCategory = (typeof POSITION_CATEGORIES)[number];

/** 포지션별 정렬 순서 (필터/그룹 정렬 시 사용) */
export const POSITION_ORDER: Record<PositionCategory, number> = {
  프론트엔드: 0,
  백엔드: 1,
  풀스택: 2,
};

/** API용 3종 포지션 필터 (백엔드와 동일) */
export const POSITION_FILTER_OPTIONS = ["전체", ...POSITION_CATEGORIES];

/** UI 필터 옵션: 이미지/화면과 동일한 전체 포지션 라벨 */
export const POSITION_DISPLAY_OPTIONS = [
  "전체",
  "서버/백엔드",
  "프론트엔드",
  "웹 풀스택",
  "안드로이드",
  "iOS",
  "머신러닝/AI",
  "데이터 엔지니어",
  "DevOps",
  "게임 클라이언트",
  "보안 엔진",
];

/** 백엔드 JobPositionUtil과 동일한 프론트엔드 키워드 */
const FRONTEND_KEYWORDS = [
  "react", "vue", "angular", "javascript", "typescript", "js", "ts",
  "next.js", "nextjs", "nuxt", "svelte", "remix", "html", "css", "scss", "sass",
  "frontend", "front-end", "프론트엔드", "프론트", "웹퍼블리싱", "ui", "ux",
];

/** 백엔드 JobPositionUtil과 동일한 백엔드 키워드 */
const BACKEND_KEYWORDS = [
  "java", "spring", "spring boot", "springboot", "kotlin", "node", "node.js", "nodejs",
  "python", "django", "flask", "fastapi", "go", "golang", "ruby", "rails", "php",
  "c#", "csharp", ".net", "asp.net", "backend", "back-end", "백엔드", "서버",
  "express", "nest", "nestjs", "graphql", "sql", "mysql", "postgresql", "mongodb",
];

/** stack을 배열로 정규화 (API에서 문자열 또는 배열로 올 수 있음) */
function normalizeStack(stack: string[] | string | null | undefined): string[] {
  if (stack == null) return [];
  if (Array.isArray(stack)) return stack.map((s) => String(s).trim()).filter(Boolean);
  const s = String(stack).trim();
  if (!s) return [];
  if (s.includes(",")) return s.split(",").map((x) => x.trim()).filter(Boolean);
  return [s];
}

/**
 * stack 배열으로부터 포지션 도출 (백엔드 JobPositionUtil.derivePosition과 동일 로직)
 * - API position이 있으면 우선 사용, 없으면 stack 기반 도출
 * - stack은 배열 또는 문자열(쉼표 구분) 모두 허용
 */
export function derivePosition(stack: string[] | string | null | undefined, apiPosition?: string | null): PositionCategory | null {
  if (apiPosition && POSITION_CATEGORIES.includes(apiPosition as PositionCategory)) {
    return apiPosition as PositionCategory;
  }
  const arr = normalizeStack(stack);
  if (arr.length === 0) return null;

  const lower = arr.map((s) => String(s).toLowerCase().trim());
  if (lower.length === 0) return null;

  const hasFrontend = lower.some((s) => FRONTEND_KEYWORDS.some((kw) => s.includes(kw)));
  const hasBackend = lower.some((s) => BACKEND_KEYWORDS.some((kw) => s.includes(kw)));

  if (hasFrontend && hasBackend) return "풀스택";
  if (hasFrontend) return "프론트엔드";
  if (hasBackend) return "백엔드";
  return null;
}

/**
 * 포지션별 정렬용: job의 포지션 순서 반환 (없으면 맨 뒤)
 * - API 포지션(프론트엔드/백엔드/풀스택)만 순서 적용, 그 외는 맨 뒤
 */
export function getPositionSortOrder(position: string | null | undefined): number {
  if (position && POSITION_CATEGORIES.includes(position as PositionCategory))
    return POSITION_ORDER[position as PositionCategory];
  return POSITION_CATEGORIES.length;
}

/** stack + API position으로 UI용 포지션 라벨 목록 도출 (필터/표시용) */
export function getPositionLabelsFromStack(
  stack: string[] | string | null | undefined,
  apiPosition?: string | null
): string[] {
  const labels: string[] = [];
  const arr = normalizeStack(stack);
  const lower = arr.map((s) => String(s).toLowerCase().trim());

  const apiPos = apiPosition?.trim();
  if (apiPos === "프론트엔드") labels.push("프론트엔드");
  if (apiPos === "백엔드") labels.push("서버/백엔드");
  if (apiPos === "풀스택") labels.push("웹 풀스택");

  const has = (keywords: string[]) => keywords.some((kw) => lower.some((s) => s.includes(kw)));
  if (!labels.includes("프론트엔드") && has(["react", "vue", "angular", "javascript", "typescript", "next", "nuxt", "svelte", "html", "css", "frontend", "프론트"])) labels.push("프론트엔드");
  if (!labels.includes("서버/백엔드") && has(["java", "spring", "kotlin", "node", "python", "django", "flask", "go", "golang", "backend", "백엔드", "서버", "express", "nestjs", "mysql", "postgresql", "mongodb"])) labels.push("서버/백엔드");
  if (!labels.includes("웹 풀스택") && labels.includes("프론트엔드") && labels.includes("서버/백엔드")) labels.push("웹 풀스택");

  if (has(["android", "안드로이드"]) && lower.some((s) => s.includes("android") || s.includes("mobile"))) labels.push("안드로이드");
  if (has(["ios", "swift", "아이오에스"])) labels.push("iOS");
  if (has(["tensorflow", "pytorch", "machine learning", "ml", "ai", "keras", "머신러닝", "딥러닝"])) labels.push("머신러닝/AI");
  if (has(["spark", "airflow", "kafka", "bigquery", "data engineer", "데이터", "etl"])) labels.push("데이터 엔지니어");
  if (has(["docker", "kubernetes", "k8s", "terraform", "ci/cd", "devops", "aws", "gcp", "azure"])) labels.push("DevOps");
  if (has(["unity", "unreal", "game", "게임", "c++", "c#"])) labels.push("게임 클라이언트");
  if (has(["security", "보안"])) labels.push("보안 엔진");

  return [...new Set(labels)];
}
