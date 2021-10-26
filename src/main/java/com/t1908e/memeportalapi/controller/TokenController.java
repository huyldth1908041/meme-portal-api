package com.t1908e.memeportalapi.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.t1908e.memeportalapi.service.TokenService;
import com.t1908e.memeportalapi.util.JwtUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
@CrossOrigin
public class TokenController {
    private final TokenService tokenService;
    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public ResponseEntity<?> getTokenHistory(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    ) {
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
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "createdAt";
        return tokenService.getInvoicesByUser(username, page - 1, limit, sortBy, order);
    }

    @RequestMapping(value = "/history", method = RequestMethod.POST)
    public ResponseEntity<?> pushMemeByToken(
            @RequestParam(name = "postId", required = false) Integer postId,
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "amountOfToken", required = false) Integer amountOfToken,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    ) {
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
        return tokenService.pushMemeByToken(username, postId, userId, amountOfToken);
    }
}
