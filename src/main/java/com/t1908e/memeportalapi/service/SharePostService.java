package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.NotificationDTO;
import com.t1908e.memeportalapi.dto.ShareDTO;
import com.t1908e.memeportalapi.entity.Invoice;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.PostShare;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.InvoiceRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.repository.PostShareRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
import com.t1908e.memeportalapi.util.FirebaseUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class SharePostService {
    private final PostShareRepository postShareRepository;
    private final AuthenticationService authenticationService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    public ResponseEntity<?> saveShare(String username, int postId) {
        HashMap<String, Object> restResponse;
        //save share post
        User sharer = authenticationService.getAppUser(username);
        if (sharer == null || sharer.getStatus() < 0) {
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
        Post sharedPost = postOptional.get();
        //Share post once a day
        List<PostShare> sharedList = postShareRepository.findAllByPostIdAndUserIdOrderByCreatedAtDesc(sharedPost.getId(), sharer.getId());
        if (sharedList != null && !sharedList.isEmpty()) {
            Date lastSharedTime = sharedList.get(0).getCreatedAt();
            if (new Date().getTime() - lastSharedTime.getTime() <= 1000 * 60 * 60 * 24) {
                restResponse = new RESTResponse.CustomError()
                        .setMessage("You has reach the share limit for this post today, try again tomorrow")
                        .setCode(HttpStatus.BAD_REQUEST.value())
                        .build();
                return ResponseEntity.badRequest().body(restResponse);
            }
        }
        try {
            PostShare postShare = new PostShare();
            postShare.setCreatedAt(new Date());
            postShare.setUser(sharer);
            postShare.setPost(sharedPost);
            postShareRepository.save(postShare);
            //send notification and token

            if (sharer.getId() != sharedPost.getUser().getId()) {
                //send token: sharer: 3 token, post creator 3 token
                User postCreator = sharedPost.getUser();
                postCreator.addToken(3);
                sharer.addToken(3);
                userRepository.save(postCreator);
                userRepository.save(sharer);
                //save invoices
                Invoice postCreatorInvoice = new Invoice("token received", "post shared", 3, postCreator);
                Invoice likerInvoice = new Invoice("token received", "share a post", 3, sharer);
                invoiceRepository.save(postCreatorInvoice);
                invoiceRepository.save(likerInvoice);
                //send noti
                NotificationDTO notificationDTO = new NotificationDTO();
                notificationDTO.setUrl("/post/".concat(String.valueOf(sharedPost.getId())));
                notificationDTO.setContent(sharer.getFullName().concat(" has shared your post and you received 3 token !"));
                notificationDTO.setStatus(1);
                notificationDTO.setThumbnail(sharer.getAvatar());
                notificationDTO.setCreatedAt(new Date());
                FirebaseUtil.sendNotification(postCreator.getAccount().getUsername(), notificationDTO);
            }
            ShareDTO shareDTO = new ShareDTO();
            shareDTO.setShareCount(sharedPost.getPostShares().size());
            if (sharedPost.getStatus() != 2) {
                //subtract token
                double newBalance = sharedPost.subTractToken(100);
                postRepository.save(sharedPost);
                if (newBalance <= 0) {
                    sharedPost.setStatus(2);
                    postRepository.save(sharedPost);
                }
            }

            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(shareDTO).build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("save share failed " + exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> checkShare(String username, int postId) {
        HashMap<String, Object> restResponse;
        //save share post
        User sharer = authenticationService.getAppUser(username);
        if (sharer == null || sharer.getStatus() < 0) {
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
        Post sharedPost = postOptional.get();
        //Share post once a day
        List<PostShare> sharedList = postShareRepository.findAllByPostIdAndUserIdOrderByCreatedAtDesc(sharedPost.getId(), sharer.getId());
        if (sharedList != null && !sharedList.isEmpty()) {
            Date lastSharedTime = sharedList.get(0).getCreatedAt();
            ShareDTO.CheckShareDTO checkShareDTO;
            if (new Date().getTime() - lastSharedTime.getTime() <= 1000 * 60 * 60 * 24) {
                checkShareDTO = new ShareDTO.CheckShareDTO(false);
            } else {
                checkShareDTO = new ShareDTO.CheckShareDTO(true);
            }
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setData(checkShareDTO)
                    .build();
            return ResponseEntity.ok().body(restResponse);
        } else {
            ShareDTO.CheckShareDTO checkShareDTO = new ShareDTO.CheckShareDTO(true);
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setData(checkShareDTO)
                    .build();
            return ResponseEntity.ok().body(restResponse);
        }
    }
}
