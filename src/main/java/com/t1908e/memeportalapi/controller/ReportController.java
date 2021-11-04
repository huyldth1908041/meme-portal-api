package com.t1908e.memeportalapi.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.t1908e.memeportalapi.dto.ReportDTO;
import com.t1908e.memeportalapi.enums.ReportType;
import com.t1908e.memeportalapi.service.ReportService;
import com.t1908e.memeportalapi.util.JwtUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import com.t1908e.memeportalapi.util.RESTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@CrossOrigin
public class ReportController {

    private final ReportService reportService;

    @RequestMapping(value = "/posts", method = RequestMethod.POST)
    public ResponseEntity<?> createPostReport(
            @Valid @RequestBody ReportDTO.ReportPostDTO reportPostDTO,
            BindingResult bindingResult,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    ) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Save report failed");
        }
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new RESTResponse
                    .CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Required token in header")
                    .build());
        }

        String accessToken = token.replace("Bearer", "").trim();
        DecodedJWT decodedJWT = JwtUtil.getDecodedJwt(accessToken);
        String username = decodedJWT.getSubject();
        ReportDTO.CreateReportDTO createReportDTO = new ReportDTO.CreateReportDTO();
        createReportDTO.setContent(reportPostDTO.getContent());
        createReportDTO.setTargetId(reportPostDTO.getPostId());
        createReportDTO.setType(ReportType.POST_REPORT);
        return reportService.createReport(createReportDTO, username);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getPostLikes(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "status", required = false) Integer status
    ) {
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "id";
        if(status == null) status = 1;
        ReportType reportType = ReportType.POST_REPORT;
        if (type == null) {
            reportType = ReportType.POST_REPORT;
        } else if (type.equals("post")) {
            reportType = ReportType.POST_REPORT;
        } else if (type.equals("user")) {
            reportType = ReportType.USER_REPORT;
        }
        return reportService.getListReport(page - 1, limit, sortBy, order, reportType, status);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getReportDetail(@PathVariable(value = "id") int id) {
        return reportService.getReportDetail(id);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteReport(@PathVariable(value = "id") int id) {
        return reportService.deleteReport(id);
    }

    @RequestMapping("/resolve")
    public ResponseEntity<?> resolveReport(@RequestBody @Valid ReportDTO.ResolveReportDTP dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Save report failed");
        }
        return reportService.resolveReport(dto.getListIds());
    }
}
