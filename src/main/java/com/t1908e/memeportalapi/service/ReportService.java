package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.NotificationDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.ReportDTO;
import com.t1908e.memeportalapi.entity.*;
import com.t1908e.memeportalapi.repository.InvoiceRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.repository.ReportRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
import com.t1908e.memeportalapi.util.FirebaseUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final AuthenticationService authenticationService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    public ResponseEntity<?> createReportPost(ReportDTO.CreateReportDTO createReportDTO, String usernameReport) {
        HashMap<String, Object> restResponse = new HashMap<>();
        if(createReportDTO.getTargetId() != 1){// not a report post
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Target value must be 1  when report post !").build();
            return ResponseEntity.badRequest().body(restResponse);
        }

        User reporter = authenticationService.getAppUser(usernameReport);
        if (reporter == null || reporter.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Post> targetOptional  = postRepository.findById( Math.toIntExact(createReportDTO.getTargetId()));
            if (!targetOptional.isPresent()) {
                restResponse = new RESTResponse.CustomError()
                        .setCode(HttpStatus.NOT_FOUND.value())
                        .setMessage("Post not found").build();
                return ResponseEntity.badRequest().body(restResponse);
            }
        Post existPost = targetOptional.get();
        Report newReport = new Report();
        newReport.setType(createReportDTO.getType());
        newReport.setTargetId(createReportDTO.getTargetId());
        newReport.setContent(createReportDTO.getContent());
        newReport.setCreatedAt(new Date());
        newReport.setUpdatedAt(new Date());
        newReport.setStatus(0); //0 pennding , 1 done
        newReport.setUser(reporter);
        try {
            Report savedReport = reportRepository.save(newReport);
            if (savedReport.getStatus() == 0) {
                NotificationDTO notification = new NotificationDTO();
                notification.setContent("You have new report to check");
                notification.setThumbnail(existPost.getImage());
                notification.setStatus(1);
                notification.setCreatedAt(new Date());
                notification.setUrl("/post/".concat(String.valueOf(savedReport.getId())));
                FirebaseUtil.sendNotification("admin@admin.com", notification);
            }
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new ReportDTO(savedReport)).build();
            return ResponseEntity.ok().body(restResponse);

        } catch (Exception exception) {
            exception.printStackTrace();
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage(exception.getMessage()).build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> createReportUser(ReportDTO.CreateReportDTO createReportDTO, String usernameReport) {
        HashMap<String, Object> restResponse = new HashMap<>();
        if(createReportDTO.getTargetId() != 2){// not a report user
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Target value must be 1  when report post !").build();
            return ResponseEntity.badRequest().body(restResponse);
        }

        User reporter = authenticationService.getAppUser(usernameReport);
        if (reporter == null || reporter.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        long targetId = createReportDTO.getTargetId();
        Optional<User> targetOptional  = userRepository.findById(targetId);
        if (!targetOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        User existUser= targetOptional.get();
        Report newReport = new Report();
        newReport.setType(createReportDTO.getType());
        newReport.setTargetId(createReportDTO.getTargetId());
        newReport.setContent(createReportDTO.getContent());
        newReport.setCreatedAt(new Date());
        newReport.setUpdatedAt(new Date());
        newReport.setStatus(0); //0 pennding , 1 done
        newReport.setUser(reporter);
        try {
            Report savedReport = reportRepository.save(newReport);
            if (savedReport.getStatus() == 0) {
                NotificationDTO notification = new NotificationDTO();
                notification.setContent("You have new report to check");
                notification.setThumbnail(existUser.getAvatar());
                notification.setStatus(1);
                notification.setCreatedAt(new Date());
                notification.setUrl("/user/".concat(String.valueOf(savedReport.getId())));
                FirebaseUtil.sendNotification("admin@admin.com", notification);
            }
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new ReportDTO(savedReport)).build();
            return ResponseEntity.ok().body(restResponse);

        } catch (Exception exception) {
            exception.printStackTrace();
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage(exception.getMessage()).build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> getReportDetail(int reportId,String username){
        HashMap<String, Object> restResponse = new HashMap<>();
        User user = authenticationService.getAppUser(username);
        if (user == null || user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (user.getAccount().getRole().getName() != "admin") {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_ACCEPTABLE.value())
                    .setMessage("User do not have permission !").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Report> optionalReport = reportRepository.findById(reportId);
        if (!optionalReport.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("Report not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Report  report = optionalReport.get();
        if(report.getType() == 1){ //report post
            Optional<Post> targetOptional = postRepository.findById( Math.toIntExact(report.getTargetId()));
            if (!targetOptional.isPresent() || targetOptional.get().getStatus() == 0) { //deactive
                restResponse = new RESTResponse.CustomError()
                        .setCode(HttpStatus.NOT_FOUND.value())
                        .setMessage("ReportTarget not found or had been delete !").build();
                return ResponseEntity.badRequest().body(restResponse);
            }
        }
        if(report.getType() == 2){ //report user
            Optional<User> targetOptional = userRepository.findById(report.getTargetId());
            if (!targetOptional.isPresent() || targetOptional.get().getStatus() == 0) { //deactive
                restResponse = new RESTResponse.CustomError()
                        .setCode(HttpStatus.NOT_FOUND.value())
                        .setMessage("ReportTarget not found or had been delete !").build();
                return ResponseEntity.badRequest().body(restResponse);
            }
        }
        restResponse = new RESTResponse.Success()
                .setMessage("success")
                .setStatus(HttpStatus.FOUND.value())
                .setData(new ReportDTO(report)).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> verifyReport(int reportId, String username) {
        HashMap<String, Object> restResponse = new HashMap<>();
        User user = authenticationService.getAppUser(username);
        if (user == null || user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (user.getAccount().getRole().getName() != "admin") {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_ACCEPTABLE.value())
                    .setMessage("User do not have permission !").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Report> optionalReport = reportRepository.findById(reportId);
        if (!optionalReport.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("Report not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Report  report = optionalReport.get();
        //xoa post
        if(report.getType() == 1){//report post
            Optional<Post> targetOptional = postRepository.findById( Math.toIntExact(report.getTargetId()));
            if (!targetOptional.isPresent() || targetOptional.get().getStatus() == 0) { //deactive
                restResponse = new RESTResponse.CustomError()
                        .setCode(HttpStatus.NOT_FOUND.value())
                        .setMessage("ReportTarget not found or had been delete !").build();
                return ResponseEntity.badRequest().body(restResponse);
            }
            Post targetPost = targetOptional.get();
            ArrayList<Integer> targetId = new ArrayList<>();
            targetId.add(Math.toIntExact(report.getTargetId()));
            int recordsAffected = postRepository.changePostStatus(targetId, -1);// -1 -> Delete
            //send notification
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setUrl("/post/".concat(String.valueOf(report.getTargetId())));
            notificationDTO.setContent("A post you create ".concat(targetPost.getTitle()).concat(" has been delete because of report!"));
            notificationDTO.setStatus(1);
            notificationDTO.setThumbnail(targetPost.getImage());
            notificationDTO.setCreatedAt(new Date());
            FirebaseUtil.sendNotification(username, notificationDTO);
        }
        // warning user
        if(report.getType() == 2){//report user
            Optional<User> targetOptional = userRepository.findById(report.getTargetId());
            if (!targetOptional.isPresent() || targetOptional.get().getStatus() == 0) { //deactive
                restResponse = new RESTResponse.CustomError()
                        .setCode(HttpStatus.NOT_FOUND.value())
                        .setMessage("ReportTarget not found or had been delete !").build();
                return ResponseEntity.badRequest().body(restResponse);
            }
            User targetUser = targetOptional.get();
            //send notification
            NotificationDTO notificationDTO = new NotificationDTO();
//            notificationDTO.setUrl("/post/".concat(String.valueOf(report.getTargetId())));
            notificationDTO.setContent("Warning !You had been report by other user base on you behaviour !");
            notificationDTO.setStatus(1);
//            notificationDTO.setThumbnail(targetPost.getImage());
            notificationDTO.setCreatedAt(new Date());
            FirebaseUtil.sendNotification(username, notificationDTO);
        }
        //gui token cho tat ca user report cung post hoac user nayf
        List<Report> listReport = reportRepository.findAllByTargetId(report.getTargetId());
        for (Report rp: listReport) {
            if (rp == null || rp.getStatus() == 1) {
                continue;
            }
            try{
                rp.setStatus(1);//done
                User userGetToken = rp.getUser();
                String userGetTokenUsername = userGetToken.getAccount().getUsername();
                //send token
                double newTokenBalance = user.addToken(20);
                userRepository.save(user);
                Invoice invoice = new Invoice();
                invoice.setAmount(20);
                invoice.setContent("Report verified");
                invoice.setName("Token received");
                invoice.setCreatedAt(new Date());
                invoice.setUpdatedAt(new Date());
                invoice.setStatus(1);
                invoice.setUser(user);
                invoiceRepository.save(invoice);
                //send notification
                NotificationDTO notificationDTO = new NotificationDTO();
//                notificationDTO.setUrl("/post/".concat(String.valueOf(post.getId())));
                notificationDTO.setContent("Your report ".concat("")
                        .concat(" has been verified! and you gained 20 tokens"));
                notificationDTO.setStatus(1);
//                notificationDTO.setThumbnail();
                notificationDTO.setCreatedAt(new Date());
                FirebaseUtil.sendNotification(userGetTokenUsername, notificationDTO);

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        restResponse = new RESTResponse.Success()
                .setMessage("success")
                .setStatus(HttpStatus.FOUND.value())
                .setData(new ReportDTO(report)).build();
        return ResponseEntity.ok().body(restResponse);
    }
}
