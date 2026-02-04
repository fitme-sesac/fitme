import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";
import fs from "fs";

/**
 * fitme/.env.docker 같은 커스텀 env 파일 파싱 (key=value, # 주석 무시)
 */
function loadEnvFile(filePath: string): Record<string, string> {
    const out: Record<string, string> = {};
    try {
        if (!fs.existsSync(filePath)) return out;
        const content = fs.readFileSync(filePath, "utf-8");
        for (const line of content.split(/\r?\n/)) {
            const trimmed = line.trim();
            if (!trimmed || trimmed.startsWith("#")) continue;
            const eq = trimmed.indexOf("=");
            if (eq <= 0) continue;
            const key = trimmed.slice(0, eq).trim();
            const value = trimmed.slice(eq + 1).trim();
            if (key) out[key] = value;
        }
    } catch {
        // ignore
    }
    return out;
}

/**
 * React Router(SPA) 경로는 Vite가 index.html을 내려줘야 한다.
 *
 * ⚠️ 브라우저 XHR(axios) 요청은 Accept 헤더에 text/html 이 없는 경우가 흔해서
 * GET 요청이 프록시로 넘어가면(예: 백엔드가 /Login, /Register 로 redirect)
 * 백엔드에 해당 GET 핸들러가 없거나 재-redirect 루프가 발생하며 500으로 보일 수 있다.
 *
 * 따라서 "페이지 경로" 로 분류한 엔드포인트는 GET이면 항상 SPA로 우회한다.
 */
const bypassSpaPageGet = (req: any) => {
    if (req.method === "GET") {
        return "/index.html";
    }
    return null;
};

export default defineConfig(({ mode }) => {
    const rootDir = path.resolve(__dirname, "..");
    const frontendDir = process.cwd();

    // 1) fitme/.env
    const envRoot = loadEnv(mode, rootDir, "");
    // 2) fitme/.env.docker (있으면 적용, 나중 것이 우선)
    const envDocker = loadEnvFile(path.join(rootDir, ".env.docker"));
    // 3) frontend/.env
    const envFrontend = loadEnv(mode, frontendDir, "");

    const env: Record<string, string> = {
        ...envRoot,
        ...envDocker,
        ...envFrontend,
    };

    // 백엔드용 변수 → 프론트 VITE_ 변수로 매핑 (VITE_*가 없을 때만)
    if (!env.VITE_KAKAO_MAP_API_KEY && env.KAKAO_MAP_API_KEY) {
        env.VITE_KAKAO_MAP_API_KEY = env.KAKAO_MAP_API_KEY;
    }
    if (!env.VITE_TOSS_CLIENT_KEY && env.TOSS_CLIENT_KEY) {
        env.VITE_TOSS_CLIENT_KEY = env.TOSS_CLIENT_KEY;
    }

    /**
     * 핵심: "프록시 타겟(서버/컨테이너 내부용)" 과
     *      "브라우저에서 사용하는 URL(VITE_)" 을 분리한다.
     *
     * - BACKEND_TARGET / AI_WORKER_TARGET : Vite dev server(노드)가 프록시할 대상
     *   (컨테이너 내부에서는 http://backend:8080 / http://ai-worker:8000 가 정상)
     *
     * - VITE_BACKEND_URL / VITE_AI_WORKER_URL : 브라우저 코드(import.meta.env)에서 사용하는 값
     *   (컨테이너 DNS는 브라우저에서 해석 안 되므로 보통 http://localhost:8080 같은 값)
     */

        // ✅ 프록시 타겟 (우선순위: process.env > env > default)
    const backendTarget =
            process.env.BACKEND_TARGET ||
            env.BACKEND_TARGET ||
            process.env.VITE_BACKEND_URL ||
            env.VITE_BACKEND_URL ||
            "http://localhost:8080";

    const aiTarget =
        process.env.AI_WORKER_TARGET ||
        env.AI_WORKER_TARGET ||
        process.env.VITE_AI_WORKER_URL ||
        env.VITE_AI_WORKER_URL ||
        "http://localhost:8000";

    return {
        plugins: [react()],
        resolve: {
            alias: {
                "@": path.resolve(__dirname, "./src"),
            },
        },
        server: {
            host: true,
            port: 5173,
            strictPort: true,
            hmr: {
                overlay: false,
            },
            proxy: {
                // ===== Backend proxies =====
                // ✅ Spring Security form 로그인: POST /Login 은 백엔드로, GET /Login 은 SPA로
                "/Login": { target: backendTarget, changeOrigin: true, bypass: bypassSpaPageGet },

                // ✅ 백엔드 form 엔드포인트들 (/User/*)은 모두 백엔드로 전달
                "/User": { target: backendTarget, changeOrigin: true },

                "/Logout": { target: backendTarget, changeOrigin: true },
                "/oauth2": { target: backendTarget, changeOrigin: true },
                "/login": { target: backendTarget, changeOrigin: true },

                // API 프록시 (백엔드)
                "/api": { target: backendTarget, changeOrigin: true },

                // 업로드된 이미지 (백엔드)
                "/images": { target: backendTarget, changeOrigin: true },

                // ===== AI worker proxies =====
                "/chatbot": { target: aiTarget, changeOrigin: true },
                "/employer-chatbot": { target: aiTarget, changeOrigin: true },
                "/resumes": { target: aiTarget, changeOrigin: true },
            },
        },
        // 외부(fitme/.env, fitme/.env.docker) + frontend/.env 에서 로드한 VITE_* 를 클라이언트에 주입
        define: Object.fromEntries(
            Object.entries(env)
                .filter(([key]) => key.startsWith("VITE_"))
                .map(([key, value]) => [`import.meta.env.${key}`, JSON.stringify(value ?? "")])
        ),
    };
});