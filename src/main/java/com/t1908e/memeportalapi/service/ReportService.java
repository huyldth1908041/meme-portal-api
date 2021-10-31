package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.NotificationDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.ReportDTO;
import com.t1908e.memeportalapi.entity.Category;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.Report;
import com.t1908e.memeportalapi.entity.User;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final AuthenticationService authenticationService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

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

        restResponse = new RESTResponse.Success()
                .setMessage("success")
                .setStatus(HttpStatus.FOUND.value())
                .setData(new ReportDTO(report)).build();
        return ResponseEntity.ok().body(restResponse);
    }

//    public ResponseEntity<?> verifyReport(int reportId, String username) {
//        HashMap<String, Object> restResponse = new HashMap<>();
//        User user = authenticationService.getAppUser(username);
//        if (user == null || user.getStatus() < 0) {
//            restResponse = new RESTResponse.CustomError()
//                    .setCode(HttpStatus.BAD_REQUEST.value())
//                    .setMessage("User not found").build();
//            return ResponseEntity.badRequest().body(restResponse);
//        }
//        if (user.getAccount().getRole().getName() != "admin") {
//            restResponse = new RESTResponse.CustomError()
//                    .setCode(HttpStatus.NOT_ACCEPTABLE.value())
//                    .setMessage("User do not have permission !").build();
//            return ResponseEntity.badRequest().body(restResponse);
//        }
//        Optional<Report> optionalReport = reportRepository.findById(reportId);
//        if (!optionalReport.isPresent()) {
//            restResponse = new RESTResponse.CustomError()
//                    .setCode(HttpStatus.NOT_FOUND.value())
//                    .setMessage("Report not found").build();
//            return ResponseEntity.badRequest().body(restResponse);
//        }
//        Report  report = optionalReport.get();
//
//        restResponse = new RESTResponse.Success()
//                .setMessage("success")
//                .setStatus(HttpStatus.FOUND.value())
//                .setData(new ReportDTO(report)).build();
//        return ResponseEntity.ok().body(restResponse);
//    }
}
