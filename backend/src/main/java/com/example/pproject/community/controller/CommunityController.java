package com.example.pproject.community.controller;

import com.example.pproject.Config.JwtUserPrincipal;
import com.example.pproject.community.dto.*;
import com.example.pproject.community.service.CommunityService;
import com.example.pproject.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 커뮤니티 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final UserRepository userRepository;

    // ============================================
    // 내 활동 관련 API
    // ============================================

    @GetMapping("/my/posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PostDTO>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.getMyPosts(memberId, page, size));
    }

    @GetMapping("/my/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CommentDTO>> getMyComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.getMyComments(memberId, page, size));
    }

    @GetMapping("/my/liked")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PostDTO>> getMyLikedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.getMyLikedPosts(memberId, page, size));
    }

    @GetMapping("/my/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommunityStatsDTO> getMyStats(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.getMyStats(memberId));
    }

    // ============================================
    // 게시글 관련 API
    // ============================================

    @GetMapping("/posts")
    public ResponseEntity<Page<PostDTO>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.getPosts(category, keyword, page, size, memberId));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostDTO> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        communityService.incrementViewCount(postId);
        return ResponseEntity.ok(communityService.getPost(postId, memberId));
    }

    @PostMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostDTO> createPost(
            @RequestBody @Valid PostCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.createPost(request, memberId));
    }

    @PutMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid PostCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.updatePost(postId, request, memberId));
    }

    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        communityService.deletePost(postId, memberId);
        return ResponseEntity.ok().build();
    }

    // ============================================
    // 댓글 관련 API
    // ============================================

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(communityService.getComments(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.createComment(postId, request, memberId));
    }

    @PutMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        return ResponseEntity.ok(communityService.updateComment(commentId, request, memberId));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        communityService.deleteComment(commentId, memberId);
        return ResponseEntity.ok().build();
    }

    // ============================================
    // 좋아요 관련 API
    // ============================================

    @PostMapping("/posts/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        boolean isLiked = communityService.toggleLike(postId, memberId);
        return ResponseEntity.ok(Map.of(
                "liked", isLiked,
                "message", isLiked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다."
        ));
    }

    @DeleteMapping("/posts/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> unlikePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        Long memberId = getMemberIdFromPrincipal(principal);
        // toggle 호출 (이미 좋아요 되어있으면 취소됨)
        boolean isLiked = communityService.toggleLike(postId, memberId);
        return ResponseEntity.ok(Map.of(
                "liked", isLiked,
                "message", isLiked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다."
        ));
    }

    // ============================================
    // 인기/추천 관련 API
    // ============================================

    @GetMapping("/popular")
    public ResponseEntity<List<PostDTO>> getPopularPosts(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(communityService.getPopularPosts(limit));
    }

    @GetMapping("/recommended-members")
    public ResponseEntity<List<Map<String, Object>>> getRecommendedMembers(
            @RequestParam(defaultValue = "3") int limit) {
        // TODO: 실제 추천 멤버 로직 구현
        // 현재는 빈 리스트 반환
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/announcements")
    public ResponseEntity<List<PostDTO>> getAnnouncements(
            @RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(communityService.getAnnouncements(limit));
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Long getMemberIdFromPrincipal(JwtUserPrincipal principal) {
        if (principal == null) {
            return null;
        }

        String userid = principal.getUserid();
        String email = principal.getEmail();

        try {
            if (userid != null && !userid.isBlank()) {
                var userOpt = userRepository.findByUserid(userid);
                if (userOpt.isPresent()) {
                    return userOpt.get().getId();
                }
            }

            if (email != null && !email.isBlank()) {
                var userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    return userOpt.get().getId();
                }
            }

            if (userid != null && userid.contains("@")) {
                var userOpt = userRepository.findByEmail(userid);
                if (userOpt.isPresent()) {
                    return userOpt.get().getId();
                }
            }
        } catch (Exception e) {
            log.warn("memberId 조회 실패: {}", e.getMessage());
        }

        return null;
    }
}
