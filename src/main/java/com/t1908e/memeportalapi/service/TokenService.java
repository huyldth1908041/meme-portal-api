package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.InvoiceDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.entity.Invoice;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.InvoiceRepository;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenService {
    private final InvoiceRepository invoiceRepository;
    private final AuthenticationService authenticationService;

    public ResponseEntity<?> getInvoicesByUser(
            String username,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse;
        User user = authenticationService.getAppUser(username);
        if (user == null || user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
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
        Page<Invoice> result = invoiceRepository.findAllByStatusAndUserId(1, user.getId(), pageInfo);
        Page<InvoiceDTO> dtoPage = result.map(new Function<Invoice, InvoiceDTO>() {
            @Override
            public InvoiceDTO apply(Invoice invoice) {
                return new InvoiceDTO(invoice);
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }
}
