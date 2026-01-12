import axios from "axios";

/**
 * Central HTTP client for backend API calls.
 * - baseURL is configured via VITE_API_BASE_URL in .env
 * - withCredentials is enabled to support HttpOnly-cookie auth (e.g., ACCESS_TOKEN)
 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  withCredentials: true,
});
