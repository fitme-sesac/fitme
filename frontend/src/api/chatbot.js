// frontend/src/api/chatbot.js
import { http } from "./http";

/**
 * @typedef {Object} ChatbotQueryRequest
 * @property {string} message
 * @property {string|null|undefined} [conversation_id]
 */

/**
 * @typedef {Object} ChatbotQueryResponse
 * @property {string} request_id
 * @property {string} conversation_id
 * @property {string} answer
 * @property {string} intent
 * @property {Object} data
 * @property {number} turn
 * @property {string} mode
 */

/**
 * @param {ChatbotQueryRequest} req
 * @returns {Promise<ChatbotQueryResponse>}
 */
export async function chatbotQuery(req) {
    const res = await http.post("/chatbot/query", req);
    return res.data;
}
