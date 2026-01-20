import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

const bypassSpaGet = (req: any) => {
    const accept = req.headers?.accept || "";
    if (req.method === "GET" && accept.includes("text/html")) {
        return "/index.html"; // GET 화면 라우팅은 SPA
    }
    return null; // POST/JSON 등은 백엔드 프록시
};

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), "");
    const target = env.VITE_BACKEND_URL || "http://localhost:8080";

    return {
        plugins: [react()],
        server: {
            proxy: {
                // GET은 SPA, POST는 백엔드
                "/Login": { target, changeOrigin: true, bypass: bypassSpaGet },
                "/Register": { target, changeOrigin: true, bypass: bypassSpaGet },
                "/MyPage": { target, changeOrigin: true, bypass: bypassSpaGet },

                "/Find_Userid": { target, changeOrigin: true, bypass: bypassSpaGet },
                "/Verify_Userid_Code": { target, changeOrigin: true, bypass: bypassSpaGet },
                "/Result_Userid": { target, changeOrigin: true, bypass: bypassSpaGet },

                "/Find_password": { target, changeOrigin: true, bypass: bypassSpaGet },
                "/Verify_Code": { target, changeOrigin: true, bypass: bypassSpaGet },
                "/New_Password": { target, changeOrigin: true, bypass: bypassSpaGet },
                "/Change_Password": { target, changeOrigin: true, bypass: bypassSpaGet },

                "/First_Social_Login": { target, changeOrigin: true, bypass: bypassSpaGet },
                "/Re_Enter_Credentials": { target, changeOrigin: true, bypass: bypassSpaGet },

                // /User/** 전체: GET은 SPA, POST는 백엔드
                "/User": { target, changeOrigin: true, bypass: bypassSpaGet },

                // 백엔드 전용
                "/Logout": { target, changeOrigin: true },
                "/oauth2": { target, changeOrigin: true },
                "/login": { target, changeOrigin: true }, // oauth2 callback
                "/api": { target, changeOrigin: true },
            },
        },
    };
});