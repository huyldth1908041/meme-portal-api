package com.t1908e.memeportalapi.controller;

import com.t1908e.memeportalapi.enums.ReportType;
import com.t1908e.memeportalapi.enums.TransactionType;
import com.t1908e.memeportalapi.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@CrossOrigin
public class TransactionController {
    private final TransactionService transactionService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> searchTx(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "creatorId", required = false) Long creatorId
    ) {
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "createdAt";
        TransactionType transactionType = null;
        if (type != null) {
            if (type.equals("transfer")) {
                transactionType = TransactionType.TRANSFER;
            } else if (type.equals("ads")) {
                transactionType = TransactionType.AD_ADS;
            } else if (type.equals("push")) {
                transactionType = TransactionType.PUSH_TO_HOT;
            } else {
                transactionType = null;
            }
        }
        return transactionService.searchTransactions(status, creatorId, transactionType, page - 1, limit, sortBy, order);
    }
}
