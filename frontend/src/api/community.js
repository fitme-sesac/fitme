import { http } from "./http";

/**
 * 커뮤니티 API 서비스
 * - 게시글, 댓글, 좋아요 관리
 */

// ============================================
// 내 활동 관련 API
// ============================================

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
 * 내가 좋아요한 게시글 조회
 * @param {number} page
 * @param {number} size
 */
export async function getMyLikedPosts(page = 0, size = 10) {
    const response = await http.get(`/api/community/my/liked?page=${page}&size=${size}`);
    return response.data;
}

/**
 * 내 커뮤니티 활동 통계 조회
 * @returns {Promise<{postCount: number, commentCount: number, receivedLikes: number}>}
 */
export async function getMyCommunityStats() {
    const response = await http.get(`/api/community/my/stats`);
    return response.data;
}

// ============================================
// 게시글 관련 API
// ============================================

/**
 * 게시글 목록 조회
 * @param {Object} params
 * @param {number} params.page
 * @param {number} params.size
 * @param {string} params.category - 카테고리 필터 (career_tips, qna, company_news, general)
 * @param {string} params.keyword - 검색 키워드
 */
export async function getPosts({ page = 0, size = 10, category = '', keyword = '' } = {}) {
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', size);
    if (category) params.append('category', category);
    if (keyword) params.append('keyword', keyword);

    const response = await http.get(`/api/community/posts?${params.toString()}`);
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

/**
 * 게시글 작성
 * @param {Object} post
 * @param {string} post.title
 * @param {string} post.content
 * @param {string} post.category
 */
export async function createPost(post) {
    const response = await http.post('/api/community/posts', post);
    return response.data;
}

/**
 * 게시글 수정
 * @param {string} postId
 * @param {Object} post
 */
export async function updatePost(postId, post) {
    const response = await http.put(`/api/community/posts/${postId}`, post);
    return response.data;
}

/**
 * 게시글 삭제
 * @param {string} postId
 */
export async function deletePost(postId) {
    const response = await http.delete(`/api/community/posts/${postId}`);
    return response.data;
}

// ============================================
// 댓글 관련 API
// ============================================

/**
 * 게시글 댓글 목록 조회
 * @param {string} postId
 */
export async function getComments(postId) {
    const response = await http.get(`/api/community/posts/${postId}/comments`);
    return response.data;
}

/**
 * 댓글 작성
 * @param {string} postId
 * @param {Object} comment
 * @param {string} comment.content
 */
export async function createComment(postId, comment) {
    const response = await http.post(`/api/community/posts/${postId}/comments`, comment);
    return response.data;
}

/**
 * 댓글 수정
 * @param {string} commentId
 * @param {Object} comment
 */
export async function updateComment(commentId, comment) {
    const response = await http.put(`/api/community/comments/${commentId}`, comment);
    return response.data;
}

/**
 * 댓글 삭제
 * @param {string} commentId
 */
export async function deleteComment(commentId) {
    const response = await http.delete(`/api/community/comments/${commentId}`);
    return response.data;
}

// ============================================
// 좋아요 관련 API
// ============================================

/**
 * 게시글 좋아요
 * @param {string} postId
 */
export async function likePost(postId) {
    const response = await http.post(`/api/community/posts/${postId}/like`);
    return response.data;
}

/**
 * 게시글 좋아요 취소
 * @param {string} postId
 */
export async function unlikePost(postId) {
    const response = await http.delete(`/api/community/posts/${postId}/like`);
    return response.data;
}

// ============================================
// 인기/추천 관련 API
// ============================================

/**
 * 인기 게시글 조회
 * @param {number} limit
 */
export async function getPopularPosts(limit = 5) {
    const response = await http.get(`/api/community/popular?limit=${limit}`);
    return response.data;
}

/**
 * 추천 멤버 조회 (활발한 활동자)
 * @param {number} limit
 */
export async function getRecommendedMembers(limit = 3) {
    const response = await http.get(`/api/community/recommended-members?limit=${limit}`);
    return response.data;
}

/**
 * 공지사항 목록 조회
 * @param {number} limit
 */
export async function getAnnouncements(limit = 3) {
    const response = await http.get(`/api/community/announcements?limit=${limit}`);
    return response.data;
}
