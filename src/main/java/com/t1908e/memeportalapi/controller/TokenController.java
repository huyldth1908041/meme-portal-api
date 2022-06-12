package com.t1908e.memeportalapi.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.t1908e.memeportalapi.dto.TransactionDTO;
import com.t1908e.memeportalapi.enums.TransactionType;
import com.t1908e.memeportalapi.service.TokenService;
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

    @RequestMapping(value = "/transfer", method = RequestMethod.POST)
    public ResponseEntity<?> transferToken(
            @Valid @RequestBody TransactionDTO.TransferTokenDTO transferTokenDTO,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            BindingResult bindingResult
    ) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new RESTResponse
                    .CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Required token in header")
                    .build());
        }
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "transfer failed");
        }

        String accessToken = token.replace("Bearer", "").trim();
        DecodedJWT decodedJWT = JwtUtil.getDecodedJwt(accessToken);
        String username = decodedJWT.getSubject();

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setTargetId(transferTokenDTO.getReceiverId());
        transactionDTO.setAmount(transferTokenDTO.getAmount());
        transactionDTO.setReason(transferTokenDTO.getReason());
        transactionDTO.setType(TransactionType.TRANSFER);
        return tokenService.createTransaction(transactionDTO, username);
    }

    @RequestMapping(value = "/push", method = RequestMethod.POST)
    public ResponseEntity<?> pushPost(
            @Valid @RequestBody TransactionDTO.PushHotDTO pushHotDTO,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            BindingResult bindingResult
    ) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new RESTResponse
                    .CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Required token in header")
                    .build());
        }
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "push failed");
        }

        String accessToken = token.replace("Bearer", "").trim();
        DecodedJWT decodedJWT = JwtUtil.getDecodedJwt(accessToken);
        String username = decodedJWT.getSubject();

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setTargetId(pushHotDTO.getPostId());
        transactionDTO.setAmount(pushHotDTO.getAmount());
        transactionDTO.setType(TransactionType.PUSH_TO_HOT);
        return tokenService.createTransaction(transactionDTO, username);
    }

    @RequestMapping(value = "/verify", method = RequestMethod.POST)
    public ResponseEntity<?> processTransaction(
            @Valid @RequestBody TransactionDTO.ProcessTransactionDTO processTransactionDTO,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            BindingResult bindingResult
    ) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new RESTResponse
                    .CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Required token in header")
                    .build());
        }
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "process tx failed");
        }

        String accessToken = token.replace("Bearer", "").trim();
        DecodedJWT decodedJWT = JwtUtil.getDecodedJwt(accessToken);
        String username = decodedJWT.getSubject();
        return tokenService.processTransaction(username, processTransactionDTO.getVerifyCode(), processTransactionDTO.getTxId());
    }


    @RequestMapping(value = "/giveToken", method = RequestMethod.PUT)
    public ResponseEntity<?> transferToken(
            @Valid @RequestBody TransactionDTO.GiveTokenDTO dto,
            BindingResult bindingResult
    ) {
        if(bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "process tx failed");
        }
        return tokenService.giveToken(dto.getUserId(), dto.getAmount());
    }

}
