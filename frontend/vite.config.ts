import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

const bypassSpaGet = (req: any) => {
    const accept = req.headers?.accept || "";
    if (req.method === "GET" && accept.includes("text/html")) {
        return "/index.html";
    }
    return null;
};

export default defineConfig(({ mode }) => {
    // Vite env (.env, .env.development 등) 로드
    // prefix="" 로 했으니 VITE_가 아닌 값도 로드되지만, 아래에서 안전하게 선택해서 씀
    const env = loadEnv(mode, process.cwd(), "");

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
        // Docker 환경에서 process.env로 전달된 VITE_ 환경변수를 클라이언트에서 사용 가능하게 함
        define: {
            'import.meta.env.VITE_TOSS_CLIENT_KEY': JSON.stringify(
                process.env.VITE_TOSS_CLIENT_KEY || env.VITE_TOSS_CLIENT_KEY || ''
            ),
        },
        server: {
            host: true,
            port: 5173,
            strictPort: true,
            proxy: {
                // ===== Backend proxies =====
                "/Login": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/Register": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/MyPage": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/Find_Userid": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/Verify_Userid_Code": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/Result_Userid": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/Find_password": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/Verify_Code": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/New_Password": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/Change_Password": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/First_Social_Login": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/Re_Enter_Credentials": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },
                "/User": { target: backendTarget, changeOrigin: true, bypass: bypassSpaGet },

                "/Logout": { target: backendTarget, changeOrigin: true },
                "/oauth2": { target: backendTarget, changeOrigin: true },
                "/login": { target: backendTarget, changeOrigin: true },
                "/api": { target: backendTarget, changeOrigin: true },

                // ===== AI worker proxies =====
                "/chatbot": { target: aiTarget, changeOrigin: true },
                "/resumes": { target: aiTarget, changeOrigin: true },
            },
        },
    };
});

