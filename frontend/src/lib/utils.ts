import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs))
}

/** API에서 받은 이미지 URL이 무효(예: cdn.example.com)면 undefined 반환 → 깨진 이미지 요청 방지 */
const INVALID_IMAGE_HOSTS = ["cdn.example.com", "example.com/photo"]
export function safeImageUrl(url: string | undefined | null): string | undefined {
    if (!url || typeof url !== "string") return undefined
    if (INVALID_IMAGE_HOSTS.some((host) => url.includes(host))) return undefined
    return url
}
