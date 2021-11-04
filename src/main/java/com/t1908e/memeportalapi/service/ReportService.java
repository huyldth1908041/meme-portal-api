package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.NotificationDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.ReportDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.*;
import com.t1908e.memeportalapi.enums.ReportType;
import com.t1908e.memeportalapi.repository.InvoiceRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.repository.ReportRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
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
public class ReportService {
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    public ResponseEntity<?> createReport(ReportDTO.CreateReportDTO createReportDTO, String username) {
        HashMap<String, Object> restResponse;
        //save like post
        User reporter = authenticationService.getAppUser(username);
        if (reporter == null || reporter.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        String message = null;
        boolean isTargetIdValid = true;
        switch (createReportDTO.getType()) {
            case POST_REPORT:
                Optional<Post> byId = postRepository.findById(createReportDTO.getTargetId());
                Post reportedPost = byId.orElse(null);
                if (reportedPost == null || reportedPost.getStatus() < 0) {
                    isTargetIdValid = false;
                    message = "post not found or has been deleted";
                }
                break;
            case USER_REPORT:
                long userId = createReportDTO.getTargetId();
                Optional<User> userById = userRepository.findById(userId);
                User reportedUser = userById.orElse(null);
                if (reportedUser == null || reportedUser.getStatus() < 0 || reportedUser.getAccount().getRole().getName().equals("admin")) {
                    isTargetIdValid = false;
                    message = "reported user is not valid";
                }
                break;
            default:
                isTargetIdValid = false;
                message = "invalid report type";
                break;
        }
        if (!isTargetIdValid) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage(message)
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            Report report = new Report();
            report.setContent(createReportDTO.getContent());
            report.setTargetId(createReportDTO.getTargetId());
            report.setStatus(0); //pending
            report.setUser(reporter);
            report.setCreatedAt(new Date());
            report.setUpdatedAt(new Date());
            report.setType(createReportDTO.getType());
            Report savedReport = reportRepository.save(report);
            //send notification to admin
            //send notification for admin
            NotificationDTO notificationDTO = new NotificationDTO();
            notificationDTO.setContent("you have new report to resolve");
            notificationDTO.setUrl("/report/".concat(String.valueOf(report.getId())));
            notificationDTO.setStatus(1);
            notificationDTO.setCreatedAt(new Date());
            FirebaseUtil.sendNotification("admin@admin.com", notificationDTO);
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new ReportDTO(savedReport)).build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("save report failed " + exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> getListReport(
            Integer page,
            Integer limit,
            String sortBy,
            String order,
            ReportType reportType,
            int status
    ) {
        HashMap<String, Object> restResponse;
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Page<Report> all = reportRepository.findAllByStatusAndType(status, reportType, pageInfo);
        Page<ReportDTO> dtoPage = all.map(new Function<Report, ReportDTO>() {
            @Override
            public ReportDTO apply(Report report) {
                return new ReportDTO(report);
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> getReportDetail(int id) {
        HashMap<String, Object> restResponse;
        Optional<Report> byId = reportRepository.findById(id);
        Report report = byId.orElse(null);
        if (report == null) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("report not found")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(new ReportDTO(report)).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> deleteReport(int id) {
        HashMap<String, Object> restResponse;
        Optional<Report> byId = reportRepository.findById(id);
        Report report = byId.orElse(null);
        if (report == null || report.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("report not found or has been deleted")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try {
            report.setStatus(-1);
            Report save = reportRepository.save(report);
            restResponse = new RESTResponse.Success()
                    .setMessage("Ok")
                    .setStatus(HttpStatus.OK.value()).build();
            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("save report failed " + exception.getMessage())
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public ResponseEntity<?> resolveReport(ArrayList<Integer> listIds) {
        List<Report> allById = reportRepository.findAllById(listIds);
        ArrayList<Report> listUpdated = new ArrayList<>();
        for (Report report : allById) {
            if (report == null || report.getStatus() < 0 || report.getStatus() == 1) {
                continue;
            }
            try {
                //update report status then transfer token and notification
                report.setStatus(1);
                listUpdated.add(report);
                User user = report.getUser();
                String username = user.getAccount().getUsername();
                //send token
                double newTokenBalance = user.addToken(10);
                userRepository.save(user);
                Invoice invoice = new Invoice();
                invoice.setAmount(10);
                invoice.setContent("Report resolved");
                invoice.setName("Token received");
                invoice.setCreatedAt(new Date());
                invoice.setUpdatedAt(new Date());
                invoice.setStatus(1);
                invoice.setUser(user);
                invoiceRepository.save(invoice);
                //send notification
                NotificationDTO notificationDTO = new NotificationDTO();
                notificationDTO.setUrl("/token/history/");
                notificationDTO.setContent("Your post report is verified an you gain 10 tokens");
                notificationDTO.setStatus(1);
                notificationDTO.setThumbnail(user.getAvatar());
                notificationDTO.setCreatedAt(new Date());
                FirebaseUtil.sendNotification(username, notificationDTO);
            } catch (Exception exception) {
                exception.printStackTrace();
                continue;
            }
        }
        List<Report> reportsUpdated = reportRepository.saveAll(listUpdated);
        HashMap<String, ?> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(String.valueOf(reportsUpdated.size()).concat(" rows updated"))
                .build();
        return ResponseEntity.ok().body(restResponse);

    }
}
