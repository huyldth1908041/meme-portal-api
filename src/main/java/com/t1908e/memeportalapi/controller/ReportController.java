package com.t1908e.memeportalapi.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.ReportDTO;
import com.t1908e.memeportalapi.service.ReportService;
import com.t1908e.memeportalapi.util.JwtUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import com.t1908e.memeportalapi.util.RESTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
@CrossOrigin
public class ReportController {
    private final ReportService reportService;

    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public ResponseEntity<?> userReportPost(
            @Valid @RequestBody ReportDTO.CreateReportDTO createReportDTO,
            BindingResult bindingResult,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Save post failed");
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
        return reportService.createReportPost(createReportDTO, username);
    }
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public ResponseEntity<?> userReportUser(
            @Valid @RequestBody ReportDTO.CreateReportDTO createReportDTO,
            BindingResult bindingResult,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Save post failed");
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
        return reportService.createReportUser(createReportDTO, username);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public ResponseEntity<?> getReportDetail(
            @PathVariable(name = "id") int id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

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
        return reportService.getReportDetail(id, username);
    }

    @RequestMapping(value = "/verify/{id}", method = RequestMethod.POST)
    public ResponseEntity<?> verifyReport(
            @PathVariable(name = "id") int id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

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
        return reportService.verifyReport(id, username);
    }
}
