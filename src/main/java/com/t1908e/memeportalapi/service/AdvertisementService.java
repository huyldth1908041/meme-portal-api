package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.AdvertisementDTO;
import com.t1908e.memeportalapi.dto.NotificationDTO;
import com.t1908e.memeportalapi.dto.TransactionDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.Advertisement;
import com.t1908e.memeportalapi.entity.Invoice;
import com.t1908e.memeportalapi.entity.Transaction;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.enums.TransactionType;
import com.t1908e.memeportalapi.repository.AdvertisementRepository;
import com.t1908e.memeportalapi.repository.InvoiceRepository;
import com.t1908e.memeportalapi.repository.TransactionRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
import com.t1908e.memeportalapi.util.FirebaseUtil;
import com.t1908e.memeportalapi.util.JwtUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import com.t1908e.memeportalapi.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor
public class AdvertisementService {
    private final AdvertisementRepository advertisementRepository;
    private final AuthenticationService authenticationService;
    private final EmailSenderService emailSenderService;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private static final double ADVERTISEMENT_PRICE = 1000;
    private static final double TAX = 1.01;

    public ResponseEntity<?> createAdvertisement(AdvertisementDTO.CreateAdvertisementDTO dto, String username) {
        //check balance -> create ads -> create tx -> send OTP
        HashMap<String, Object> restResponse;
        User creator = authenticationService.getAppUser(username);
        if (creator == null || creator.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive")
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
        List<Advertisement> createdAds = advertisementRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(creator.getId(), 2);
        if (!createdAds.isEmpty()) {

            Advertisement lastCreatedAds = createdAds.get(0);
            Date updatedAt = lastCreatedAds.getUpdatedAt();
            if (new Date().getTime() - updatedAt.getTime() < JwtUtil.ONE_DAY * 30L) {
                restResponse = new RESTResponse.CustomError()
                        .setMessage("you've reached advertisement limit for this month")
                        .setCode(HttpStatus.BAD_REQUEST.value())
                        .build();
                return ResponseEntity.badRequest().body(restResponse);
            }
        }
        try {
            //create ads
            Advertisement advertisement = new Advertisement();
            advertisement.setTitle(dto.getTitle());
            advertisement.setContent(dto.getContent());
            advertisement.setImage(dto.getImage());
            advertisement.setUrl(dto.getUrl());
            advertisement.setCreatedAt(new Date());
            advertisement.setUpdatedAt(new Date());
            advertisement.setStatus(0);
            advertisement.setUser(creator);
            Advertisement saved = advertisementRepository.save(advertisement);
            //create an tx
            //create a pending transaction
            Transaction transaction = new Transaction();
            transaction.setType(TransactionType.AD_ADS);
            transaction.setAmount(ADVERTISEMENT_PRICE);
            transaction.setUser(creator);
            transaction.setTargetId(saved.getId());
            transaction.setCreatedAt(new Date());
            transaction.setUpdatedAt(new Date());
            transaction.setStatus(0); //pending
            //generate verify code
            String verifyCode = RandomUtil.generateVerifyCode();
            transaction.setVerifyCode(verifyCode);
            //send email
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
            return ResponseEntity.internalServerError().body(new RESTResponse.CustomError()
                    .setMessage(exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }

    public ResponseEntity<?> verifyAdvertisements(ArrayList<Integer> adIds) {
        List<Advertisement> listAds = advertisementRepository.findAllById(adIds);
        List<Advertisement> listAdsUpdated = new ArrayList<>();
        for (Advertisement advert : listAds) {
            if (advert.getStatus() != 1) {
                continue;
            }
            advert.setStatus(2);
            advert.setUpdatedAt(new Date());
            listAdsUpdated.add(advert);
        }
        advertisementRepository.saveAll(listAdsUpdated);
        HashMap<String, Object> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData("Updated success ".concat(String.valueOf(listAdsUpdated.size())).concat(" rows affected")).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> deleteAd(int id) {
        HashMap<String, ?> restResponse;
        Optional<Advertisement> byId = advertisementRepository.findById(id);
        Advertisement advertisement = byId.orElse(null);
        if (advertisement == null || advertisement.getStatus() < 0 || advertisement.getStatus() == 1) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("ad not found or has been deleted or not been purchased yet")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            advertisement.setStatus(-1);
            advertisement.setUpdatedAt(new Date());
            advertisementRepository.save(advertisement);
            //return token for user
            User user = advertisement.getUser();
            user.addToken(ADVERTISEMENT_PRICE);
            userRepository.save(user);
            //save invoice
            Invoice creatorInvoice = new Invoice("Return token", "advertisement not verified", ADVERTISEMENT_PRICE, user);
            invoiceRepository.save(creatorInvoice);
            //send noti
            //send notification for admin
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setContent("your advertisement is not verified so we return you 1000 tokens");
            notificationDTO.setUrl("/token/history");
            notificationDTO.setStatus(1);
            notificationDTO.setThumbnail(advertisement.getImage());
            notificationDTO.setCreatedAt(new Date());
            FirebaseUtil.sendNotification(user.getAccount().getUsername(), notificationDTO);
            restResponse = new RESTResponse.Success()
                    .setMessage("Ads success")
                    .setStatus(HttpStatus.OK.value())
                    .setData("update success").build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(new RESTResponse.CustomError()
                    .setMessage(exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }

    public ResponseEntity<?> getAdvertisementDetail(int id) {
        HashMap<String, ?> restResponse;
        Optional<Advertisement> byId = advertisementRepository.findById(id);
        Advertisement advertisement = byId.orElse(null);
        if (advertisement == null) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("ad not found or has been deleted or not been purchased yet")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        restResponse = new RESTResponse.Success()
                .setMessage("Ads success")
                .setStatus(HttpStatus.OK.value())
                .setData(new AdvertisementDTO(advertisement)).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> getActiveAdvertisement() {
        HashMap<String, ?> restResponse;
        //return first active ads not expired
        //ads expire time = 1 DAY
        List<Advertisement> activeAds = advertisementRepository.findActiveAds();// 2 active
        if(activeAds.isEmpty()) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("No ads is active")
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .build();
            return ResponseEntity.accepted().body(restResponse);
        }
        Advertisement firstActiveAd = activeAds.get(0);
        restResponse = new RESTResponse.Success()
                .setMessage("OK")
                .setStatus(HttpStatus.OK.value())
                .setData(new AdvertisementDTO(firstActiveAd)).build();
        return ResponseEntity.ok().body(restResponse);
    }
    public ResponseEntity<?> searchListAdvertisement(
            HashMap<String, Object> params,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        Specification<Advertisement> specStatus = null;
        if (params.containsKey("status")) {
            specStatus = advertisementByStatus((int) params.get("status"));
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
        Page<Advertisement> all = advertisementRepository.findAll(specStatus, pageInfo);
        Page<AdvertisementDTO> dtoPage = all.map(new Function<Advertisement, AdvertisementDTO>() {
            @Override
            public AdvertisementDTO apply(Advertisement advertisement) {
                return new AdvertisementDTO(advertisement);
            }
        });
        HashMap<String, ?> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    private Specification<Advertisement> advertisementByStatus(Integer status) {
        return (root, query, cb) -> {
            if (status != null) {
                return cb.equal(root.get("status"), status);
            } else {
                return cb.and();
            }
        };
    }
}
