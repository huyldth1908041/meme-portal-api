package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.InvoiceDTO;
import com.t1908e.memeportalapi.dto.NotificationDTO;
import com.t1908e.memeportalapi.dto.TransactionDTO;
import com.t1908e.memeportalapi.entity.*;
import com.t1908e.memeportalapi.repository.*;
import com.t1908e.memeportalapi.util.FirebaseUtil;
import com.t1908e.memeportalapi.util.JwtUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import com.t1908e.memeportalapi.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenService {
    private static final double ADVERTISEMENT_PRICE = 1000;
    private final InvoiceRepository invoiceRepository;
    private final AuthenticationService authenticationService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PushHistoryRepository pushHistoryRepository;
    private final EmailSenderService emailSenderService;
    private final AdvertisementRepository advertisementRepository;
    private static final double TAX = 1.01;
    private static final double DEFAULT_HOT_TOKEN = 500;
    private static final double MAX_PUSH_TOKEN_AVAILABLE = DEFAULT_HOT_TOKEN * 20 / 100;

    public ResponseEntity<?> getInvoicesByUser(
            String username,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse;
        User user = authenticationService.getAppUser(username);
        if (user == null || user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive")
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
        Page<Invoice> result = invoiceRepository.findAllByStatusAndUserId(1, user.getId(), pageInfo);
        Page<InvoiceDTO> dtoPage = result.map(new Function<Invoice, InvoiceDTO>() {
            @Override
            public InvoiceDTO apply(Invoice invoice) {
                return new InvoiceDTO(invoice);
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> createTransaction(TransactionDTO transactionDTO, String creatorUserName) {
        HashMap<String, Object> restResponse;
        User creator = authenticationService.getAppUser(creatorUserName);
        if (creator == null || creator.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (creator.getTokenBalance() < transactionDTO.getAmount() * 1.01) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Not enough token for this transaction")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        boolean isTargetValid = true;
        String errorMsg = null;
        switch (transactionDTO.getType()) {
            case TRANSFER:
                Optional<User> byId = userRepository.findById(transactionDTO.getTargetId());
                User user = byId.orElse(null);
                if (user == null || user.getStatus() < 0) {
                    isTargetValid = false;
                    errorMsg = "receiver not found or has been deleted";
                } else if (user.getId() == creator.getId()) {
                    isTargetValid = false;
                    errorMsg = "can not transfer token to yourself";
                }
                break;
            case AD_ADS:
                break;
            case BUY_BADGE:
                break;
            case PUSH_TO_HOT:
                Optional<Post> postOptional = postRepository.findById((int) transactionDTO.getTargetId());
                Post post = postOptional.orElse(null);
                if (post == null || post.getStatus() < 0) {
                    isTargetValid = false;
                    errorMsg = "pushed post not found";
                    break;
                }
                if (post.getStatus() == 2) {
                    isTargetValid = false;
                    errorMsg = "can not push a hot post";
                    break;
                }
                if (post.getUpHotTokenNeeded() < transactionDTO.getAmount()) {
                    isTargetValid = false;
                    errorMsg = "push amount exceeds the push available";
                    break;
                }
                break;
            case BUY_DISPLAY_NAME_COLOR:
                break;
            default:
                break;
        }
        if (!isTargetValid) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage(errorMsg)
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            //create a pending transaction
            Transaction transaction = new Transaction();

            transaction.setType(transactionDTO.getType());
            transaction.setAmount(transactionDTO.getAmount());
            transaction.setReason(transactionDTO.getReason());
            transaction.setUser(creator);
            transaction.setTargetId(transactionDTO.getTargetId());
            transaction.setCreatedAt(new Date());
            transaction.setUpdatedAt(new Date());
            transaction.setStatus(0); //pending
            //generate verify code
            String verifyCode = RandomUtil.generateVerifyCode();
            transaction.setVerifyCode(verifyCode);
            //send emai
            String message = "your transaction verify code is ".concat(verifyCode);
            String subject = "Token Transaction Verify code on Hài Code [".concat(verifyCode).concat("]");
            emailSenderService
                    .sendSimpleEmail(creator.getAccount().getUsername(), message, subject);
            //save transaction
            Transaction savedTx = transactionRepository.save(transaction);
            restResponse = new RESTResponse.Success()
                    .setMessage("A verify code has sent to user email")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new TransactionDTO(savedTx)).build();
            return ResponseEntity.ok().body(restResponse);

        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(new RESTResponse.CustomError()
                    .setMessage(exception.getMessage())
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    public ResponseEntity<?> processTransaction(String username, String verifyCode, int txId) {
        HashMap<String, Object> restResponse;
        Optional<Transaction> byId = transactionRepository.findById(txId);
        Transaction transaction = byId.orElse(null);
        if (transaction == null || transaction.getStatus() != 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("transaction not exist or not in pending")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        User creator = authenticationService.getAppUser(username);
        if (creator == null || creator.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (creator.getId() != transaction.getUser().getId()) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("permission denied: can not process other's transaction")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        //check verify code
        if (!verifyCode.equals(transaction.getVerifyCode())) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Wrong verify transaction code")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        //process the transaction
        try {
            transaction.setStatus(1); //verified == active
            transactionRepository.save(transaction);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage(exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }

        switch (transaction.getType()) {
            case TRANSFER:
                return processTransferToken(transaction);
            case AD_ADS:
                return processAddAds(transaction);
            case BUY_BADGE:
                return processBuyBadge(transaction);
            case PUSH_TO_HOT:
                return processPushToHot(transaction);
            case BUY_DISPLAY_NAME_COLOR:
                return processBuyDisplayNameColor(transaction);
            default:
                restResponse = new RESTResponse.CustomError()
                        .setMessage("Invalid transaction type")
                        .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build();
                return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    private ResponseEntity<?> processBuyDisplayNameColor(Transaction transaction) {
        return null;
    }

    private ResponseEntity<?> processPushToHot(Transaction transaction) {
        HashMap<String, Object> restResponse;
        if (transaction == null || transaction.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Transaction has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Post> postOptional = postRepository.findById((int) transaction.getTargetId());
        Post pushedPost = postOptional.orElse(null);
        if (pushedPost == null || pushedPost.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Post has been deleted or not found")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (pushedPost.getStatus() == 2) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Can not push a hot post")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        User pusher = transaction.getUser();
        if (pusher == null || pusher.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("pusher not found or has been deavtived")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        double pushedAmount = transaction.getAmount();
        double transactionTokenAmount = pushedAmount * TAX;
        if (pusher.getTokenBalance() < transactionTokenAmount) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Not enough token for this transaction")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            pusher.subtractToken(transactionTokenAmount);
            double newPostTokenBalance = pushedPost.subTractToken(pushedAmount);
            userRepository.save(pusher);
            postRepository.save(pushedPost);
            //check to up hot
            if (newPostTokenBalance <= 0) {
                pushedPost.setStatus(2);
                postRepository.save(pushedPost);
            }
            //save invoice
            Invoice pusherInvoice = new Invoice("push post", "use token to push post", transactionTokenAmount, pusher);
            invoiceRepository.save(pusherInvoice);
            //save push history
            PushHistory pushHistory = new PushHistory();
            pushHistory.setStatus(1); //active
            pushHistory.setCreatedAt(new Date());
            pushHistory.setUpdateAt(new Date());
            pushHistory.setUser(pusher);
            pushHistory.setPost(pushedPost);
            pushHistory.setTokenAmount(pushedAmount);
            pushHistoryRepository.save(pushHistory);
            //save tx
            transaction.setStatus(2); //done
            transaction.setUpdatedAt(new Date());
            Transaction updatedTx = transactionRepository.save(transaction);
            //send notification for post creator
            if (pusher.getId() != pushedPost.getUser().getId()) {
                NotificationDTO notificationDTO = new NotificationDTO();
                notificationDTO.setContent(pusher.getFullName().concat(" has pushed ".concat(String.valueOf(pushedAmount)).concat(" token to your post")));
                notificationDTO.setUrl("/post/".concat(String.valueOf(pushedPost.getId())));
                notificationDTO.setStatus(1);
                notificationDTO.setThumbnail(pusher.getAvatar());
                notificationDTO.setCreatedAt(new Date());
                FirebaseUtil.sendNotification(pushedPost.getUser().getAccount().getUsername(), notificationDTO);
            }
            restResponse = new RESTResponse.Success()
                    .setMessage("Push success")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new TransactionDTO(updatedTx)).build();
            return ResponseEntity.ok().body(restResponse);

        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage(exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    private ResponseEntity<?> processBuyBadge(Transaction transaction) {
        return null;
    }

    private ResponseEntity<?> processAddAds(Transaction transaction) {
        //update ads status -> 1, subtract user balance, save invoice, send notifications
        HashMap<String, Object> restResponse;
        if (transaction == null || transaction.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Transaction has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        User creator = transaction.getUser();
        if (creator == null || creator.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("sender not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (creator.getTokenBalance() < ADVERTISEMENT_PRICE * TAX) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Not enough token for this transaction")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Advertisement> byId = advertisementRepository.findById((int) transaction.getTargetId());
        Advertisement advertisement = byId.orElse(null);
        if(advertisement == null || advertisement.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Advertisement not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        List<Advertisement> createdAds = advertisementRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(creator.getId(), 2);
        if(!createdAds.isEmpty()) {
            Advertisement lastCreatedAds = createdAds.get(0);
            Date updatedAt = lastCreatedAds.getUpdatedAt();
            if(new Date().getTime() - updatedAt.getTime() < JwtUtil.ONE_DAY * 30L) {
                restResponse = new RESTResponse.CustomError()
                        .setMessage("you've reached advertisement limit for this month")
                        .setCode(HttpStatus.BAD_REQUEST.value())
                        .build();
                return ResponseEntity.badRequest().body(restResponse);
            }
        }
        try {
            advertisement.setStatus(1); // đã thanh toán đang pending
            advertisement.setUpdatedAt(new Date());
            advertisementRepository.save(advertisement);
            //subtract user balance
            creator.subtractToken(ADVERTISEMENT_PRICE * TAX);
            userRepository.save(creator);
            //update tx
            transaction.setStatus(2); //done
            Transaction updateTx = transactionRepository.save(transaction);
            //save invoice
            Invoice creatorInvoice = new Invoice("Create Advertisement", "use token to create advertisement", ADVERTISEMENT_PRICE * TAX, creator);
            invoiceRepository.save(creatorInvoice);
            //send notification for admin
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setContent("you have new advertisement to verify");
            notificationDTO.setUrl("/ads/".concat(String.valueOf(advertisement.getId())));
            notificationDTO.setStatus(1);
            notificationDTO.setThumbnail(advertisement.getImage());
            notificationDTO.setCreatedAt(new Date());
            FirebaseUtil.sendNotification("admin@admin.com", notificationDTO);

            restResponse = new RESTResponse.Success()
                    .setMessage("Ads success")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new TransactionDTO(updateTx)).build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage(exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    private ResponseEntity<?> processTransferToken(Transaction transaction) {
        HashMap<String, Object> restResponse;
        if (transaction == null || transaction.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Transaction has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        User sender = transaction.getUser();
        if (sender == null || sender.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("sender not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        long receiverId = transaction.getTargetId();
        Optional<User> receiverOptional = userRepository.findById(receiverId);
        User receiver = receiverOptional.orElse(null);
        if (receiver == null || receiver.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("receiver not found or has been deactived")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (sender.getId() == receiver.getId()) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("can not transfer token to yourself")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        double amount = transaction.getAmount();
        if (sender.getTokenBalance() < amount * TAX) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("not enough token")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            sender.subtractToken(amount * TAX);
            receiver.addToken(amount);
            userRepository.save(sender);
            userRepository.save(receiver);
            //save invoice
            Invoice senderInvoice = new Invoice("send token", "transfer token to ".concat(receiver.getFullName()), amount * TAX, sender);
            Invoice receiverInvoice = new Invoice("receive token", sender.getFullName().concat(" send token"), amount, receiver);
            invoiceRepository.save(senderInvoice);
            invoiceRepository.save(receiverInvoice);
            //update transaction
            transaction.setStatus(2); //done
            transaction.setUpdatedAt(new Date());
            Transaction updatedTx = transactionRepository.save(transaction);
            //send notification to receiver
            //send notification
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setContent(sender.getFullName().concat(" has transfer to you ".concat(String.valueOf(amount)).concat(" token")));
            notificationDTO.setUrl("/token/history");
            notificationDTO.setStatus(1);
            notificationDTO.setThumbnail(sender.getAvatar());
            notificationDTO.setCreatedAt(new Date());
            FirebaseUtil.sendNotification(receiver.getAccount().getUsername(), notificationDTO);
            restResponse = new RESTResponse.Success()
                    .setMessage("transfer success")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new TransactionDTO(updatedTx)).build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage(exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }

    }

    public ResponseEntity<?> giveToken(long userId, double amount) {
        HashMap<String, Object> restResponse;
        Optional<User> userOptional = userRepository.findById(userId);
        if(!userOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("sender not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            User user = userOptional.get();
            user.addToken(amount);
            userRepository.save(user);

            Invoice invoice = new Invoice("receive token", "Admin give token", amount, user);
            invoiceRepository.save(invoice);

            //send notification
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setContent("Admin has sent to you ".concat(String.valueOf(amount)).concat(" tokens"));
            notificationDTO.setUrl("/token/history");
            notificationDTO.setStatus(1);
            notificationDTO.setThumbnail(user.getAvatar());
            notificationDTO.setCreatedAt(new Date());
            FirebaseUtil.sendNotification(user.getAccount().getUsername(), notificationDTO);

            restResponse = new RESTResponse.Success()
                    .setMessage("transfer success")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new InvoiceDTO(invoice)).build();
            return ResponseEntity.ok().body(restResponse);

        }catch (Exception err) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage(err.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }

    }
}
