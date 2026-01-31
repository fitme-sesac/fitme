import { http } from "./http";

/**
 * 내 게시글 조회
 * @param {number} page 
 * @param {number} size 
 */
export async function getMyPosts(page = 0, size = 10) {
    const response = await http.get(`/api/community/my/posts?page=${page}&size=${size}`);
    return response.data;
}

/**
 * 내 댓글 조회
 * @param {number} page 
 * @param {number} size 
 */
export async function getMyComments(page = 0, size = 10) {
    const response = await http.get(`/api/community/my/comments?page=${page}&size=${size}`);
    return response.data;
}

/**
 * 게시글 상세 조회
 * @param {string} postId 
 */
export async function getPost(postId) {
    const response = await http.get(`/api/community/posts/${postId}`);
    return response.data;
}
