package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.CommentDTO;
import com.t1908e.memeportalapi.dto.NotificationDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.*;
import com.t1908e.memeportalapi.repository.*;
import com.t1908e.memeportalapi.util.FirebaseUtil;
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
    private final InvoiceRepository invoiceRepository;

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
        User postCreator = commentedPost.getUser();
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
            NotificationDTO notificationDTO = new NotificationDTO();
            if (savedComment.getRepliedCommentId() != 0) {
                //reply a comment -> send notification for user has comment that be replied
                //get user that has comment replied
                Optional<Comment> byId = commentRepository.findById(savedComment.getRepliedCommentId());
                Comment parentComment = byId.orElse(null);
                if (parentComment.getUser().getId() != commenter.getId()) {
                    //send token: post creator: 2, commenter: 2, replier: 2
                    User parentCommentUser = parentComment.getUser();
                    //check if post creator is replier
                    if(postCreator.getId() != commenter.getId()) {
                        postCreator.addToken(2);
                        Invoice postCreatorInvoice = new Invoice("token received", "post commented", 2, postCreator);
                        userRepository.save(postCreator);
                        invoiceRepository.save(postCreatorInvoice);
                    }
                    commenter.addToken(2);
                    parentCommentUser.addToken(2);
                    userRepository.save(commenter);
                    userRepository.save(parentCommentUser);
                    //save invoices
                    Invoice replierInvoice = new Invoice("token received", "reply a comment", 2, commenter);
                    Invoice commenterInvoice = new Invoice("token received", "comment replied", 2, parentCommentUser);

                    invoiceRepository.save(replierInvoice);
                    invoiceRepository.save(commenterInvoice);
                    //send notification
                    notificationDTO.setUrl("/post/".concat(String.valueOf(commentedPost.getId())));
                    notificationDTO.setContent(commenter.getFullName().concat(" has replied your comment and ypu gained 2 tokens!"));
                    notificationDTO.setStatus(1);
                    notificationDTO.setThumbnail(commenter.getAvatar());
                    notificationDTO.setCreatedAt(new Date());
                    FirebaseUtil.sendNotification(parentCommentUser.getAccount().getUsername(), notificationDTO);
                }
            } else {
                if (commenter.getId() != commentedPost.getUser().getId()) {
                    //comment at a post -> send notification to creator
                    //send token: post creator: 2 commenter: 2
                    postCreator.addToken(2);
                    commenter.addToken(2);
                    userRepository.save(postCreator);
                    userRepository.save(commenter);
                    //save invoice
                    Invoice commenterInvoice = new Invoice("token received", "comment a post", 2, commenter);
                    Invoice postCreatorInvoice = new Invoice("token received", "post commented", 2, postCreator);
                    invoiceRepository.save(commenterInvoice);
                    invoiceRepository.save(postCreatorInvoice);
                    //send notification
                    notificationDTO.setUrl("/post/".concat(String.valueOf(commentedPost.getId())));
                    notificationDTO.setContent(commenter.getFullName().concat(" has commented your post and you gained 2 tokens"));
                    notificationDTO.setStatus(1);
                    notificationDTO.setThumbnail(commenter.getAvatar());
                    notificationDTO.setCreatedAt(new Date());
                    FirebaseUtil.sendNotification(commentedPost.getUser().getAccount().getUsername(), notificationDTO);
                }
            }
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
            //send notification and token
            if (liker.getId() != comment.getUser().getId()) {
                //send token 2 for commenter 1 for liker
                User commenter = comment.getUser();
                commenter.addToken(2);
                liker.addToken(1);
                userRepository.save(commenter);
                userRepository.save(liker);
                //save invoice
                Invoice commenterInvoice = new Invoice("token received", "comment liked", 2, commenter);
                Invoice likerInvoice = new Invoice("token received", "like a comment", 1, liker);
                invoiceRepository.save(commenterInvoice);
                invoiceRepository.save(likerInvoice);
                //send notification
                NotificationDTO notificationDTO = new NotificationDTO();
                notificationDTO.setContent(liker.getFullName().concat(" has liked your comment and you gained 2 tokens"));
                notificationDTO.setUrl("/post/".concat(String.valueOf(comment.getPost().getId())));
                notificationDTO.setStatus(1);
                notificationDTO.setThumbnail(liker.getAvatar());
                notificationDTO.setCreatedAt(new Date());
                FirebaseUtil.sendNotification(commenter.getAccount().getUsername(), notificationDTO);
            }
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
