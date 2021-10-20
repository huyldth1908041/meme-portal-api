package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.TopCreatorDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.*;
import com.t1908e.memeportalapi.repository.PostLikeRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
import org.springframework.data.domain.*;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.repository.CategoryRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.specification.PostSpecificationBuilder;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
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
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;

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
        if (creator == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post newPost = new Post();
        newPost.setTitle(postDTO.getTitle());
        newPost.setDescription(postDTO.getDescription());
        newPost.setImage(postDTO.getImage());
        newPost.setUpHotTokenNeeded(0);
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
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new PostDTO(savedPost)).build();
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
        if (editor == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
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
                    .setData(new PostDTO(savedPost)).build();
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
                return new PostDTO(post);
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> verifyPosts(ArrayList<Integer> postIds) {
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
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(new PostDTO(post)).build();
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
        List<User> activeUsers = userRepository.findAllByStatus(1);
        HashMap<Integer, User> userByPostsCount = new HashMap<>();
        //O(n^2)
        for (int i = 0; i < activeUsers.size(); i++) {
            User user = activeUsers.get(i);
            Set<Post> posts = user.getPosts();
            List<Post> activePosts = posts.stream().filter(item -> item.getStatus() > 0).collect(Collectors.toList());
            userByPostsCount.put(activePosts.size(), user);
        }
        TreeMap<Integer, User> sorted = new TreeMap<>(Collections.reverseOrder());
        //O(Log n)
        sorted.putAll(userByPostsCount);
        ArrayList<TopCreatorDTO> topCreatorDTOs = new ArrayList<>();
        //O(n)
        for (Map.Entry<Integer, User> entry : sorted.entrySet()) {
            Integer count = entry.getKey();
            User user = entry.getValue();
            if (topCreatorDTOs.size() == 5) {
                break;
            }
            topCreatorDTOs.add(new TopCreatorDTO(user, count));
        }
        HashMap<String, Object> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(topCreatorDTOs).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> LikeAPost(int postId, String username) {
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
            postLikeDTO.setLikeCount(post.getPostLikes().size());
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
        Set<PostLike> postLikes = post.getPostLikes();
        List<Long> listUserIds = postLikes.stream().map(item -> item.getUser().getId()).collect(Collectors.toList());
        Specification<User> likedSpec = userIdIn(listUserIds);
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));

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
}
