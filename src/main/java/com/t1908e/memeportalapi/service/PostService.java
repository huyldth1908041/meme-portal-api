package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.*;
import com.t1908e.memeportalapi.entity.*;
import com.t1908e.memeportalapi.repository.*;
import com.t1908e.memeportalapi.util.FirebaseUtil;
import org.springframework.data.domain.*;
import com.t1908e.memeportalapi.specification.PostSpecificationBuilder;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {
    private static final double DEFAULT_UP_HOT_TOKEN_NEEDED = 500;
    private static final double TOKEN_WHEN_LIKED = 50;
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final InvoiceRepository invoiceRepository;
    private final PushHistoryRepository pushHistoryRepository;
    private final CommentRepository commentRepository;
    private final PostShareRepository postShareRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;

    public ResponseEntity<?> savePost(PostDTO.CreatePostDTO postDTO, String creatorUsername) {
        HashMap<String, Object> restResponse = new HashMap<>();
        if (creatorUsername.isEmpty() || postDTO == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage("Null pointer exception").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Category> categoryOptional = categoryRepository.findById(postDTO.getCategoryId());
        if (!categoryOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Category not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Category postCategory = categoryOptional.get();
        User creator = authenticationService.getAppUser(creatorUsername);
        if (creator == null || creator.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found or has been deactived").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post newPost = new Post();
        newPost.setTitle(postDTO.getTitle());
        newPost.setDescription(postDTO.getDescription());
        newPost.setImage(postDTO.getImage());
        newPost.setUpHotTokenNeeded(DEFAULT_UP_HOT_TOKEN_NEEDED);
        if (creator.getAccount().getRole().getName().equals("admin")) {
            newPost.setStatus(1); // admin create post no need to verify
        } else {
            newPost.setStatus(0); // 0 -> PENDING
        }
        newPost.setCreatedAt(new Date());
        newPost.setUpdatedAt(new Date());
        newPost.setCategory(postCategory);
        newPost.setUser(creator);
        try {
            Post savedPost = postRepository.save(newPost);
            if (savedPost.getStatus() == 0) {
                //send notification to all admin
                //get admin role
                Optional<Role> admin = roleRepository.findByName("admin");
                if(!admin.isPresent()) {
                    restResponse = new RESTResponse.CustomError()
                            .setCode(HttpStatus.BAD_REQUEST.value())
                            .setMessage("Not found admin role for push notification").build();
                    return ResponseEntity.badRequest().body(restResponse);
                }
                List<Account> listAdmins = accountRepository.findAllByRoleId(admin.get().getId());
                if(listAdmins.size() == 0) {
                    restResponse = new RESTResponse.CustomError()
                            .setCode(HttpStatus.BAD_REQUEST.value())
                            .setMessage("Not found any admin in system for push notification").build();
                    return ResponseEntity.badRequest().body(restResponse);
                }
                for (int i = 0; i < listAdmins.size(); i++) {
                    Account adminAccount = listAdmins.get(i);
                    NotificationDTO notification = new NotificationDTO();
                    notification.setContent("You have new post to verify");
                    notification.setThumbnail(savedPost.getImage());
                    notification.setStatus(1);
                    notification.setCreatedAt(new Date());
                    notification.setUrl("/post/".concat(String.valueOf(savedPost.getId())));
                    FirebaseUtil.sendNotification(adminAccount.getUsername(), notification);
                }

            }
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new PostDTO(savedPost)).build(); //khong can tra ve count....
            return ResponseEntity.ok().body(restResponse);

        } catch (Exception exception) {
            exception.printStackTrace();
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage(exception.getMessage()).build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> editPost(PostDTO.CreatePostDTO postDTO, String creatorUsername, int postId) {
        HashMap<String, Object> restResponse = new HashMap<>();
        if (creatorUsername.isEmpty() || postDTO == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage("Null pointer exception").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Category> categoryOptional = categoryRepository.findById(postDTO.getCategoryId());
        if (!categoryOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Category not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Category postCategory = categoryOptional.get();
        User editor = authenticationService.getAppUser(creatorUsername);
        if (editor == null || editor.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found or has been de-activated").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (editor.getAccount().getRole().getName().equals("user")) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Permission denied").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Post> postOptional = postRepository.findById(postId);
        if (!postOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("Post not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post existPost = postOptional.get();
        existPost.setTitle(postDTO.getTitle());
        existPost.setDescription(postDTO.getDescription());
        existPost.setImage(postDTO.getImage());
        existPost.setUpdatedAt(new Date());
        existPost.setCategory(postCategory);
        try {
            Post savedPost = postRepository.save(existPost);
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new PostDTO(savedPost)).build(); //không cần trả về count
            return ResponseEntity.ok().body(restResponse);

        } catch (Exception exception) {
            exception.printStackTrace();
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage(exception.getMessage()).build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> searchListPost(
            HashMap<String, Object> params,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse = new HashMap<>();
        PostSpecificationBuilder builder = new PostSpecificationBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            builder.with(key, ":", value);
        }
        Specification<Post> spec = builder.build();
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Page<Post> all = postRepository.findAll(spec, pageInfo);
        Page<PostDTO> dtoPage = all.map(new Function<Post, PostDTO>() {
            @Override
            public PostDTO apply(Post post) {
                int id = post.getId();
                PostDTO postDTO = new PostDTO(post);//cần trả về count
                postDTO.setLikeCounts(postLikeRepository.countByPostId(id));
                postDTO.setCommentCounts(commentRepository.countByPostIdAndStatusGreaterThan(id, 0));
                postDTO.setShareCounts(postShareRepository.countByPostId(id));
                postDTO.setPushCount(pushHistoryRepository.countByPostIdAndStatusGreaterThan(id, 0));
                postDTO.setListLiked(postLikeRepository.findListUserIdLiked(id));
                return postDTO;
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> verifyPosts(ArrayList<Integer> postIds) {
        //get list users
        List<Post> allById = postRepository.findAllById(postIds);
        for (Post post : allById) {
            if (post == null || post.getStatus() == 1) {
                continue;
            }
            try {
                User user = post.getUser();
                String username = user.getAccount().getUsername();
                //send token
                double newTokenBalance = user.addToken(20);
                userRepository.save(user);
                Invoice invoice = new Invoice();
                invoice.setAmount(20);
                invoice.setContent("Post verified");
                invoice.setName("Token received");
                invoice.setCreatedAt(new Date());
                invoice.setUpdatedAt(new Date());
                invoice.setStatus(1);
                invoice.setUser(user);
                invoiceRepository.save(invoice);
                //send notification
                NotificationDTO notificationDTO = new NotificationDTO();
                notificationDTO.setUrl("/post/".concat(String.valueOf(post.getId())));
                notificationDTO.setContent("Your post ".concat(post.getTitle())
                        .concat(" has been verified! and you gained 20 tokens"));
                notificationDTO.setStatus(1);
                notificationDTO.setThumbnail(post.getImage());
                notificationDTO.setCreatedAt(new Date());
                FirebaseUtil.sendNotification(username, notificationDTO);
            } catch (Exception exception) {
                exception.printStackTrace();
                continue;
            }
        }
        int recordsAffected = postRepository.changePostStatus(postIds, 1);// 1 -> ACTIVE
        HashMap<String, Object> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData("Updated success ".concat(String.valueOf(recordsAffected)).concat(" rows affected")).build();
        return ResponseEntity.ok().body(restResponse);
    }


    public ResponseEntity<?> getPostDetail(int id) {
        HashMap<String, Object> restResponse = new HashMap<>();
        Optional<Post> postOptional = postRepository.findById(id);
        Post post = postOptional.orElse(null);
        if (post == null) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("This post is not exist or had been delete !")
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        PostDTO postDTO = new PostDTO(post);
        postDTO.setCommentCounts(commentRepository.countByPostIdAndStatusGreaterThan(id, 0));
        postDTO.setPushCount(pushHistoryRepository.countByPostIdAndStatusGreaterThan(id, 0));
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(postDTO).build(); //cần trả về count
        return ResponseEntity.ok().body(restResponse);
    }


    public ResponseEntity<?> deletePosts(ArrayList<Integer> postIds) {
        int recordsAffected = postRepository.changePostStatus(postIds, -1);// -1 -> DE-ACTIVE
        HashMap<String, Object> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData("Updated success ".concat(String.valueOf(recordsAffected)).concat(" rows affected")).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> getTopCreator() {
        List<Object[]> queryResults = userRepository.findTopCreator();
        List<TopCreatorDTO> topCreatorDTOS = new ArrayList<>();
        for (Object[] result : queryResults) {
            BigInteger userId = (BigInteger) result[0];
            String fullName = String.valueOf(result[1]);
            String avatar = String.valueOf(result[2]);
            BigInteger postCount = (BigInteger) result[3];
            TopCreatorDTO topCreatorDTO = new TopCreatorDTO(userId.longValue(), fullName, avatar, postCount.intValue());
            topCreatorDTOS.add(topCreatorDTO);
        }
        HashMap<String, Object> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(topCreatorDTOS).build();
        return ResponseEntity.ok().body(restResponse);

    }

    public ResponseEntity<?> likeAPost(int postId, String username) {
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
        Optional<Post> postOptional = postRepository.findById(postId);
        if (!postOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post post = postOptional.get();
        //check if user has already liked post
        Optional<PostLike> existLikeOptional = postLikeRepository.findPostLikeByPostIdAndUserId(postId, liker.getId());
        if (existLikeOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("User already liked this post")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            PostLike postLike = new PostLike();
            postLike.setPost(post);
            postLike.setUser(liker);
            postLike.setCreatedAt(new Date());
            postLikeRepository.save(postLike);
            PostDTO.PostLikeDTO postLikeDTO = new PostDTO.PostLikeDTO();
            postLikeDTO.setHasLikedYet(true);
            int likeCount = post.getPostLikes().size();
            postLikeDTO.setLikeCount(likeCount);
            //send notification and token when only when user like other user's post
            if (liker.getId() != post.getUser().getId()) {
                //send token: post creator 5 token, liker 1 token
                User postCreator = post.getUser();
                postCreator.addToken(5);
                liker.addToken(1);
                userRepository.save(postCreator);
                userRepository.save(liker);
                //save invoices
                Invoice postCreatorInvoice = new Invoice("token received", "post liked", 5, postCreator);
                Invoice likerInvoice = new Invoice("token received", "like a post", 1, liker);
                invoiceRepository.save(postCreatorInvoice);
                invoiceRepository.save(likerInvoice);
                //send notification
                NotificationDTO notificationDTO = new NotificationDTO();
                notificationDTO.setUrl("/post/".concat(String.valueOf(post.getId())));
                notificationDTO.setContent(liker.getFullName().concat(" has liked your post and you received 5 token !"));
                notificationDTO.setStatus(1);
                notificationDTO.setThumbnail(liker.getAvatar());
                notificationDTO.setCreatedAt(new Date());
                FirebaseUtil.sendNotification(postCreator.getAccount().getUsername(), notificationDTO);
            }
            if (post.getStatus() == 2) {
                restResponse = new RESTResponse.Success()
                        .setMessage("Ok")
                        .setStatus(HttpStatus.CREATED.value())
                        .setData(postLikeDTO).build();
                return ResponseEntity.ok().body(restResponse);
            }
            //check like count to push to hot or subtract up hot token needed
            long activeUsersCount = userRepository.countAllByStatusGreaterThan(0);
            double likeNeededToUpHot = (double) activeUsersCount * 20 / 100;
            if (likeCount >= likeNeededToUpHot) {
                //push to hot
                post.setStatus(2);
                postRepository.save(post);
                restResponse = new RESTResponse.Success()
                        .setMessage("Ok")
                        .setStatus(HttpStatus.CREATED.value())
                        .setData(postLikeDTO).build();
                return ResponseEntity.ok().body(restResponse);
            }
            //subtract token
            double newBalance = post.subTractToken(TOKEN_WHEN_LIKED);
            postRepository.save(post);
            if (newBalance <= 0) {
                post.setStatus(2);
                postRepository.save(post);
            }
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(postLikeDTO).build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("save Like failed " + exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }

    }

    public ResponseEntity<?> getPostLikes(
            int postId,
            Integer page,
            Integer limit,
            String sortBy,
            String order,
            String username
    ) {
        HashMap<String, Object> restResponse = new HashMap<>();
        //get list user liked post
        Optional<Post> byId = postRepository.findById(postId);
        if (!byId.isPresent() || byId.get().getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Post not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post post = byId.get();
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Set<PostLike> postLikes = post.getPostLikes();
        if (postLikes == null || postLikes.isEmpty()) {
            PostDTO.ListPostLikeDTO listPostLikeDTO = new PostDTO.ListPostLikeDTO();
            listPostLikeDTO.setLikedList(new PageImpl<UserDTO>(new ArrayList<UserDTO>()));
            listPostLikeDTO.setHasLikedYet(false);
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.OK.value())
                    .setData(listPostLikeDTO).build();
            return ResponseEntity.ok().body(restResponse);
        }
        List<Long> listUserIds = postLikes.stream().map(item -> item.getUser().getId()).collect(Collectors.toList());
        Specification<User> likedSpec = userIdIn(listUserIds);
        Page<User> all = userRepository.findAll(likedSpec, pageInfo);
        Page<UserDTO> dtoPage = all.map(new Function<User, UserDTO>() {
            @Override
            public UserDTO apply(User user) {
                return new UserDTO(user);
            }
        });
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
            Optional<PostLike> existOptional = postLikeRepository.findPostLikeByPostIdAndUserId(postId, currentUser.getId());
            if (existOptional.isPresent()) {
                userHasLikedThisPost = true;
            }
        }
        PostDTO.ListPostLikeDTO listPostLikeDTO = new PostDTO.ListPostLikeDTO();
        listPostLikeDTO.setLikedList(dtoPage);
        listPostLikeDTO.setHasLikedYet(userHasLikedThisPost);
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(listPostLikeDTO).build();
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

    public ResponseEntity<?> getLikeCount(int id, String username) {
        HashMap<String, Object> restResponse = new HashMap<>();
        //get like count
        Optional<Post> byId = postRepository.findById(id);
        if (!byId.isPresent() || byId.get().getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Post not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post post = byId.get();
        Set<PostLike> postLikes = post.getPostLikes();
        int likeCount = postLikes.size();
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
            Optional<PostLike> existOptional = postLikeRepository.findPostLikeByPostIdAndUserId(id, currentUser.getId());
            if (existOptional.isPresent()) {
                userHasLikedThisPost = true;
            }
        }
        PostDTO.PostLikeDTO postLikeDTO = new PostDTO.PostLikeDTO();
        postLikeDTO.setLikeCount(likeCount);
        postLikeDTO.setHasLikedYet(userHasLikedThisPost);
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(postLikeDTO).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> getPushedList(
            int postId,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse;
        //get list user liked post
        Optional<Post> byId = postRepository.findById(postId);
        if (!byId.isPresent() || byId.get().getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Post not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post post = byId.get();
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Page<PushHistory> listPushed
                = pushHistoryRepository.findAllByPostIdAndStatusGreaterThan(post.getId(), 0, pageInfo);
        //convert to DTO
        Page<PushHistoryDTO> dtoPage = listPushed.map(new Function<PushHistory, PushHistoryDTO>() {
            @Override
            public PushHistoryDTO apply(PushHistory pushHistory) {
                return new PushHistoryDTO(pushHistory);
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> makeHotPost(int postId) {
        HashMap<String, ?> restResponse;
        Optional<Post> byId = postRepository.findById(postId);
        Post post = byId.orElse(null);
        if (post == null || post.getStatus() < 0 || post.getStatus() == 2) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Post not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            post.setStatus(2);
            Post save = postRepository.save(post);
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new PostDTO(save)).build(); //không cần trả về count
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("save Like failed " + exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> makeNew(int postId) {
        HashMap<String, ?> restResponse;
        Optional<Post> byId = postRepository.findById(postId);
        Post post = byId.orElse(null);
        if (post == null || post.getStatus() < 0 || post.getStatus() == 1) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Post not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            post.setStatus(1);
            post.setUpHotTokenNeeded(DEFAULT_UP_HOT_TOKEN_NEEDED);
            Post save = postRepository.save(post);
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new PostDTO(save)).build(); //không cần trả về count
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("update post failed " + exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

}
