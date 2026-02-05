package com.example.pproject.community.service;

import com.example.pproject.community.dto.*;
import com.example.pproject.community.entity.*;
import com.example.pproject.community.repository.*;
import com.example.pproject.notification.service.NotificationService;
import com.example.pproject.user.entity.UserEntity;
import com.example.pproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ============================================
    // 게시글 관련
    // ============================================

    public Page<PostDTO> getPosts(String category, String keyword, int page, int size, Long memberId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CommunityPost> posts;

        if (category != null && !category.isBlank() && keyword != null && !keyword.isBlank()) {
            posts = postRepository.searchByCategoryAndKeyword(category, keyword, pageable);
        } else if (category != null && !category.isBlank()) {
            posts = postRepository.findByCategory(category, pageable);
        } else if (keyword != null && !keyword.isBlank()) {
            posts = postRepository.searchByKeyword(keyword, pageable);
        } else {
            posts = postRepository.findAllNotDeleted(pageable);
        }

        return posts.map(post -> {
            PostDTO dto = PostDTO.from(post);
            if (memberId != null) {
                dto.setIsLiked(likeRepository.existsByPostIdAndMemberId(post.getId(), memberId));
            }
            return dto;
        });
    }

    public PostDTO getPost(Long postId, Long memberId) {
        CommunityPost post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(() -> new IllegalStateException("게시글을 찾을 수 없습니다."));

        PostDTO dto = PostDTO.from(post);
        if (memberId != null) {
            dto.setIsLiked(likeRepository.existsByPostIdAndMemberId(postId, memberId));
        }
        return dto;
    }

    @Transactional
    public PostDTO createPost(PostCreateRequest request, Long memberId) {
        UserEntity author = userRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        CommunityPost post = CommunityPost.builder()
                .author(author)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .build();

        postRepository.save(post);
        log.info("게시글 생성: {} by {}", post.getTitle(), author.getUsername());

        return PostDTO.from(post);
    }

    @Transactional
    public PostDTO updatePost(Long postId, PostCreateRequest request, Long memberId) {
        CommunityPost post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(() -> new IllegalStateException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        if (request.getTitle() != null)
            post.setTitle(request.getTitle());
        if (request.getContent() != null)
            post.setContent(request.getContent());
        if (request.getCategory() != null)
            post.setCategory(request.getCategory());

        postRepository.save(post);
        log.info("게시글 수정: {}", post.getTitle());

        return PostDTO.from(post);
    }

    @Transactional
    public void deletePost(Long postId, Long memberId) {
        CommunityPost post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(() -> new IllegalStateException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        post.setDeletedAt(Instant.now());
        postRepository.save(post);
        log.info("게시글 삭제: {}", post.getTitle());
    }

    @Transactional
    public void incrementViewCount(Long postId) {
        CommunityPost post = postRepository.findByIdAndNotDeleted(postId).orElse(null);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        }
    }

    // ============================================
    // 댓글 관련
    // ============================================

    public List<CommentDTO> getComments(Long postId) {
        return commentRepository.findByPostId(postId).stream()
                .map(CommentDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDTO createComment(Long postId, CommentCreateRequest request, Long memberId) {
        CommunityPost post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(() -> new IllegalStateException("게시글을 찾을 수 없습니다."));

        UserEntity author = userRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .author(author)
                .content(request.getContent())
                .build();

        commentRepository.save(comment);

        // 댓글 수 증가
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        // 게시글 작성자에게 댓글 알림 발송 (본인 글에 본인이 댓글 달면 제외)
        Long postAuthorId = post.getAuthor().getId();
        if (!postAuthorId.equals(memberId)) {
            try {
                notificationService.sendNotification(
                        postAuthorId,
                        "COMMUNITY_COMMENT",
                        Map.of(
                                "postId", postId,
                                "postTitle", post.getTitle() != null ? post.getTitle() : "게시글",
                                "commenterName", author.getUsername() != null ? author.getUsername() : "익명",
                                "commentPreview", request.getContent().length() > 30
                                        ? request.getContent().substring(0, 30) + "..."
                                        : request.getContent()));
                log.info("댓글 알림 발송: postAuthor={}, commenter={}", postAuthorId, memberId);
            } catch (Exception e) {
                log.warn("댓글 알림 발송 실패: {}", e.getMessage());
            }
        }

        log.info("댓글 생성: postId={}, by {}", postId, author.getUsername());

        return CommentDTO.from(comment);
    }

    @Transactional
    public CommentDTO updateComment(Long commentId, CommentCreateRequest request, Long memberId) {
        CommunityComment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalStateException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getId().equals(memberId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        comment.setContent(request.getContent());
        commentRepository.save(comment);

        return CommentDTO.from(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        CommunityComment comment = commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalStateException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getId().equals(memberId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        // 댓글 수 감소
        CommunityPost post = comment.getPost();
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);

        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
    }

    // ============================================
    // 좋아요 관련
    // ============================================

    @Transactional
    public boolean toggleLike(Long postId, Long memberId) {
        CommunityPost post = postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(() -> new IllegalStateException("게시글을 찾을 수 없습니다."));

        UserEntity member = userRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        boolean exists = likeRepository.existsByPostIdAndMemberId(postId, memberId);

        if (exists) {
            // 좋아요 취소
            likeRepository.deleteByPostIdAndMemberId(postId, memberId);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
            return false;
        } else {
            // 좋아요 추가
            CommunityLike like = CommunityLike.builder()
                    .post(post)
                    .member(member)
                    .build();
            likeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
            return true;
        }
    }

    // ============================================
    // 내 활동 관련
    // ============================================

    public Page<PostDTO> getMyPosts(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByAuthorId(memberId, pageable)
                .map(PostDTO::from);
    }

    public Page<CommentDTO> getMyComments(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findByAuthorId(memberId, pageable)
                .map(CommentDTO::from);
    }

    public Page<PostDTO> getMyLikedPosts(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return likeRepository.findLikedPostsByMemberId(memberId, pageable)
                .map(PostDTO::from);
    }

    public CommunityStatsDTO getMyStats(Long memberId) {
        long postCount = postRepository.countByAuthorIdAndDeletedAtIsNull(memberId);
        long commentCount = commentRepository.countByAuthorIdAndDeletedAtIsNull(memberId);
        long receivedLikes = postRepository.sumLikesByAuthorId(memberId);

        return CommunityStatsDTO.builder()
                .postCount(postCount)
                .commentCount(commentCount)
                .receivedLikes(receivedLikes)
                .build();
    }

    // ============================================
    // 인기/추천 관련
    // ============================================

    public List<PostDTO> getPopularPosts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return postRepository.findPopularPosts(pageable).stream()
                .map(PostDTO::from)
                .collect(Collectors.toList());
    }

    public List<PostDTO> getAnnouncements(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return postRepository.findPinnedPosts(pageable).stream()
                .map(PostDTO::from)
                .collect(Collectors.toList());
    }
}
