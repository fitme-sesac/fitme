import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const target = process.env.VITE_BACKEND_URL || "http://localhost:8080";

const bypassSpaGet = (req: any) => {
    const accept = req.headers?.accept || "";
    if (req.method === "GET" && accept.includes("text/html")) {
        return "/index.html"; // SPA 라우팅으로 처리
    }
    return null; // 그 외(POST/JSON 등)는 백엔드 프록시
};

export default defineConfig({
    plugins: [react()],
    server: {
        proxy: {
            // (기존 유지) legacy 경로들: GET은 SPA, POST는 백엔드
            "/Login": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/Register": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/MyPage": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/Find_Userid": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/Result_Userid": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/Verify_Userid_Code": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/VerifyUserIdCode": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/ResultUserId": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/Find_password": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/Verify_Code": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/New_Password": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/Change_Password": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/First_Social_Login": { target, changeOrigin: true, bypass: bypassSpaGet },
            "/Re_Enter_Credentials": { target, changeOrigin: true, bypass: bypassSpaGet },

            "/User": { target, changeOrigin: true, bypass: bypassSpaGet },

            // 백엔드가 반드시 처리해야 하는 것들(그대로 프록시)
            "/oauth2": { target, changeOrigin: true },
            "/login": { target, changeOrigin: true }, // oauth2 callback 등
            "/api": { target, changeOrigin: true },
        },
    },
});
