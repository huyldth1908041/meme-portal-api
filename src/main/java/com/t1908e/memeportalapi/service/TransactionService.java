package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.AdvertisementDTO;
import com.t1908e.memeportalapi.dto.TransactionDTO;
import com.t1908e.memeportalapi.entity.Advertisement;
import com.t1908e.memeportalapi.entity.Transaction;
import com.t1908e.memeportalapi.enums.TransactionType;
import com.t1908e.memeportalapi.repository.TransactionRepository;
import com.t1908e.memeportalapi.util.RESTResponse;
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
import java.util.HashMap;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public ResponseEntity<?> searchTransactions(
            Integer status,
            Long userId,
            TransactionType transactionType,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse = new HashMap<>();
        Specification<Transaction> spec = null;
        Specification<Advertisement> specStatus = null;
        spec = transactionsByStatus(status).and(transactionsByCreatorId(userId)).and(transactionsByType(transactionType));
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Page<Transaction> all = transactionRepository.findAll(spec, pageInfo);
        Page<TransactionDTO> dtoPage = all.map(new Function<Transaction, TransactionDTO>() {
            @Override
            public TransactionDTO apply(Transaction transaction) {
                return new TransactionDTO(transaction);
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    private Specification<Transaction> transactionsByStatus(Integer status) {
        return (root, query, cb) -> {
            if (status != null) {
                return cb.equal(root.get("status"), status);
            } else {
                return cb.and();
            }
        };
    }

    private Specification<Transaction> transactionsByCreatorId(Long userId) {
        return (root, query, cb) -> {
            if (userId != null) {
                return cb.equal(root.get("userId"), userId);
            } else {
                return cb.and();
            }
        };
    }

    private Specification<Transaction> transactionsByType(TransactionType type) {
        return (root, query, cb) -> {
            if (type != null) {
                return cb.equal(root.get("type"), type);
            } else {
                return cb.and();
            }
        };
    }
}
