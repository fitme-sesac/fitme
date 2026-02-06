// frontend/src/api/employer-chatbot.js
import { http } from "./http";

/**
 * @typedef {Object} EmployerChatbotQueryRequest
 * @property {string} message
 * @property {string|null|undefined} [conversation_id]
 * @property {number|null|undefined} [employer_id]
 */

/**
 * @typedef {Object} EmployerChatbotQueryResponse
 * @property {string} request_id
 * @property {string} conversation_id
 * @property {string} answer
 * @property {string} intent
 * @property {Object} data
 * @property {number} turn
 * @property {string} mode
 */

/**
 * @param {EmployerChatbotQueryRequest} req
 * @returns {Promise<EmployerChatbotQueryResponse>}
 */
export async function employerChatbotQuery(req) {
    const res = await http.post("/employer-chatbot/query", req);
    return res.data;
}
