package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.InvoiceDTO;
import com.t1908e.memeportalapi.dto.NotificationDTO;
import com.t1908e.memeportalapi.dto.TransactionDTO;
import com.t1908e.memeportalapi.entity.Invoice;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.Transaction;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.InvoiceRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.repository.TransactionRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
import com.t1908e.memeportalapi.sms.SmsRequest;
import com.t1908e.memeportalapi.util.FirebaseUtil;
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
import java.util.Optional;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenService {
    private final InvoiceRepository invoiceRepository;
    private final AuthenticationService authenticationService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TwilioSmsSender twilioSmsSender;
    private final TransactionRepository transactionRepository;

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
        switch (transactionDTO.getType()) {
            case TRANSFER:
                Optional<User> byId = userRepository.findById(transactionDTO.getTargetId());
                User user = byId.orElse(null);
                if (user == null || user.getStatus() < 0) {
                    isTargetValid = false;
                } else if(user.getId() == creator.getId()) {
                    isTargetValid = false;
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
                }
                break;
            case BUY_DISPLAY_NAME_COLOR:
                break;
            default:
                break;
        }
        if (!isTargetValid) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("transaction target not exist or has been deactived")
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
            //save transaction
            Transaction savedTx = transactionRepository.save(transaction);
            //send sms
            String message = "your verify code is ".concat(savedTx.getVerifyCode());
            twilioSmsSender.sendSms(new SmsRequest(creator.getPhone(), message));
            restResponse = new RESTResponse.Success()
                    .setMessage("A verify code has sent to user phone")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new TransactionDTO(transaction)).build();
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
        return null;
    }

    private ResponseEntity<?> processBuyBadge(Transaction transaction) {
        return null;
    }

    private ResponseEntity<?> processAddAds(Transaction transaction) {
        return null;
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
        if (sender.getTokenBalance() < amount * 1.01) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("not enough token")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            sender.subtractToken(amount * 1.01);
            receiver.addToken(amount);
            userRepository.save(sender);
            userRepository.save(receiver);
            //save invoice
            Invoice senderInvoice = new Invoice("send token", "transfer token to ".concat(receiver.getFullName()), amount * 1.01, sender);
            Invoice receiverInvoice = new Invoice("receive token", receiver.getFullName().concat(" send token"), amount, receiver);
            invoiceRepository.save(senderInvoice);
            invoiceRepository.save(receiverInvoice);
            //update transaction
            transaction.setStatus(2); //done
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
}
