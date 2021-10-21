package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.CommentDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.*;
import com.t1908e.memeportalapi.repository.*;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor

public class CommentService {
    private final PostRepository postRepository;
    private final AuthenticationService authenticationService;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    public ResponseEntity<?> commentAPost(int postId, CommentDTO.CreateCommentDTO createCommentDTO, String username) {
        HashMap<String, Object> restResponse = new HashMap<>();
        User commenter = authenticationService.getAppUser(username);
        if (commenter == null || commenter.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        //get post
        Optional<Post> postOptional = postRepository.findById(postId);
        Post commentedPost = postOptional.orElse(null);
        if (commentedPost == null || commentedPost.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("post not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            Comment comment = new Comment();
            comment.setUser(commenter);
            comment.setContent(createCommentDTO.getContent());
            comment.setPost(commentedPost);
            comment.setStatus(1);
            comment.setCreatedAt(new Date());
            comment.setUpdatedAt(new Date());
            if (createCommentDTO.getReplyCommentId() != null) {
                Optional<Comment> byId = commentRepository.findById(createCommentDTO.getReplyCommentId());
                Comment parentComment = byId.orElse(null);
                if (parentComment == null || parentComment.getStatus() < 0) {
                    restResponse = new RESTResponse.CustomError()
                            .setMessage("reply to a comment that does not exist or has been deleted")
                            .setCode(HttpStatus.BAD_REQUEST.value())
                            .build();
                    return ResponseEntity.badRequest().body(restResponse);
                }
                comment.setRepliedCommentId(createCommentDTO.getReplyCommentId());
            }
            Comment savedComment = commentRepository.save(comment);
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new CommentDTO(savedComment, 0)).build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("save Like failed " + exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }

    }

    public ResponseEntity<?> getListComments(
            int postId,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse = new HashMap<>();
        Optional<Post> postOptional = postRepository.findById(postId);
        Post commentedPost = postOptional.orElse(null);
        if (commentedPost == null || commentedPost.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("post not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Page<Comment> resultComment = commentRepository.findAllByRepliedCommentIdAndPostId(0, postId, pageInfo);
        Page<CommentDTO> dtoPage = resultComment.map(new Function<Comment, CommentDTO>() {
            @Override
            public CommentDTO apply(Comment comment) {
                return new CommentDTO(comment, commentRepository.countRepliedComment(comment.getId()));
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.CREATED.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> getRepliedCommentOfAComment(
            int commentId, Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse = new HashMap<>();
        Optional<Comment> byId = commentRepository.findById(commentId);
        Comment comment = byId.orElse(null);
        if (comment == null || comment.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("comment not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }

        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Page<Comment> all = commentRepository.findAllByRepliedCommentId(commentId, pageInfo);
        Page<CommentDTO> dtoPage = all.map(new Function<Comment, CommentDTO>() {
            @Override
            public CommentDTO apply(Comment comment) {
                return new CommentDTO(comment, commentRepository.countRepliedComment(comment.getId()));
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.CREATED.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> likeAComment(int commentId, String username) {
        HashMap<String, Object> restResponse = new HashMap<>();
        //save like post
        User liker = authenticationService.getAppUser(username);
        if (liker == null || liker.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        if (!commentOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("comment not found or has been deactive")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Comment comment = commentOptional.get();
        //check if user has already liked comment
        Optional<CommentLike> existLikeOptional = commentLikeRepository.findCommentLikeByCommentIdAndUserId(commentId, liker.getId());
        if (existLikeOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("User already liked this comment")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            CommentLike commentLike = new CommentLike();
            commentLike.setComment(comment);
            commentLike.setUser(liker);
            commentLike.setLikedAt(new Date());
            commentLikeRepository.save(commentLike);
            CommentDTO.CommentLikeDTO commentLikeDTO = new CommentDTO.CommentLikeDTO();
            commentLikeDTO.setHasLikedYet(true);
            commentLikeDTO.setLikeCount(comment.getCommentLikes().size());
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(commentLikeDTO).build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("save Like failed " + exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> getLikeCount(int id, String username) {
        HashMap<String, Object> restResponse = new HashMap<>();
        //get like count
        Optional<Comment> byId = commentRepository.findById(id);
        if (!byId.isPresent() || byId.get().getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Comment not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Comment comment = byId.get();
        Set<CommentLike> commentLikes = comment.getCommentLikes();
        int likeCount = commentLikes.size();
        //check if liked yet or not
        boolean userHasLikedThisPost = false;
        if (username != null) {
            User currentUser = authenticationService.getAppUser(username);
            if (currentUser == null) {
                restResponse = new RESTResponse.CustomError()
                        .setMessage("username not found or has been deleted")
                        .setCode(HttpStatus.BAD_REQUEST.value())
                        .build();
                return ResponseEntity.badRequest().body(restResponse);
            }
            Optional<CommentLike> existOptional = commentLikeRepository.findCommentLikeByCommentIdAndUserId(id, currentUser.getId());
            if (existOptional.isPresent()) {
                userHasLikedThisPost = true;
            }
        }
        CommentDTO.CommentLikeDTO commentLikeDTO = new CommentDTO.CommentLikeDTO();
        commentLikeDTO.setLikeCount(likeCount);
        commentLikeDTO.setHasLikedYet(userHasLikedThisPost);
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(commentLikeDTO).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> getCommentLikes(
            int commentId,
            Integer page,
            Integer limit,
            String sortBy,
            String order,
            String username
    ) {
        HashMap<String, Object> restResponse = new HashMap<>();
        //get list user liked post
        Optional<Comment> byId = commentRepository.findById(commentId);
        if (!byId.isPresent() || byId.get().getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Comment not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Comment comment = byId.get();
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Set<CommentLike> commentLikes = comment.getCommentLikes();
        if (commentLikes == null || commentLikes.isEmpty()) {
            CommentDTO.ListCommentLikeDTO listCommentLikeDTO = new CommentDTO.ListCommentLikeDTO();
            listCommentLikeDTO.setLikedList(new PageImpl<UserDTO>(new ArrayList<UserDTO>()));
            listCommentLikeDTO.setHasLikedYet(false);
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.OK.value())
                    .setData(listCommentLikeDTO).build();
            return ResponseEntity.ok().body(restResponse);
        }
        List<Long> listUserIds = commentLikes.stream().map(item -> item.getUser().getId()).collect(Collectors.toList());
        Specification<User> likedSpec = userIdIn(listUserIds);
        Page<User> all = userRepository.findAll(likedSpec, pageInfo);
        Page<UserDTO> dtoPage = all.map(new Function<User, UserDTO>() {
            @Override
            public UserDTO apply(User user) {
                return new UserDTO(user);
            }
        });
        //check if liked yet or not
        boolean userHasLikedThisComment = false;
        if (username != null) {
            User currentUser = authenticationService.getAppUser(username);
            if (currentUser == null) {
                restResponse = new RESTResponse.CustomError()
                        .setMessage("username not found or has been deleted")
                        .setCode(HttpStatus.BAD_REQUEST.value())
                        .build();
                return ResponseEntity.badRequest().body(restResponse);
            }
            Optional<CommentLike> existOptional = commentLikeRepository.findCommentLikeByCommentIdAndUserId(commentId, currentUser.getId());
            if (existOptional.isPresent()) {
                userHasLikedThisComment = true;
            }
        }
        CommentDTO.ListCommentLikeDTO listCommentLikeDTO = new CommentDTO.ListCommentLikeDTO();
        listCommentLikeDTO.setLikedList(dtoPage);
        listCommentLikeDTO.setHasLikedYet(userHasLikedThisComment);
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(listCommentLikeDTO).build();
        return ResponseEntity.ok().body(restResponse);
    }

    private Specification<User> userIdIn(List<Long> types) {
        return (root, query, cb) -> {
            if (types != null && !types.isEmpty()) {
                return root.<String>get("id").in(types);
            } else {
                // always-true predicate, means that no filtering would be applied
                return cb.and();
            }
        };
    }
}
