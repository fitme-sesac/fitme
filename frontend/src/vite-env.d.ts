/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_TOSS_CLIENT_KEY: string;
    readonly VITE_KAKAO_MAP_API_KEY: string;
    readonly VITE_API_BASE_URL?: string;
    readonly VITE_BACKEND_URL?: string;
    readonly VITE_AI_WORKER_URL?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
