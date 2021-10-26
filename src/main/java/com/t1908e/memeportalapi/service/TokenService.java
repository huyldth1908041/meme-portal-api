package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.InvoiceDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.entity.Invoice;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.InvoiceRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
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
import java.util.Optional;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenService {
    private final InvoiceRepository invoiceRepository;
    private final AuthenticationService authenticationService;
    private final PostRepository postRepository;
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
    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<?> pushMemeByToken(String username, Integer postId, long userId, double amountOfToken){
        HashMap<String, Object> restResponse;
        User user = authenticationService.getAppUser(username);
        if (user == null || user.getStatus() < 0) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("username not found or has been deactive !")
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if(user.getTokenBalance() < amountOfToken){
            restResponse = new RESTResponse.CustomError()
                    .setMessage("User's balance is not enough !")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Post> postUpVoteOptional= postRepository.findById(postId);
        Post postUpVote = postUpVoteOptional.orElse(null);
        if(postUpVote ==null || postUpVote.getStatus() != 1){
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Post not exist or had been delete !")
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        try{
            postUpVote.setUpHotTokenNeeded(postUpVote.getUpHotTokenNeeded()-amountOfToken);
            user.setTokenBalance(user.getTokenBalance() - amountOfToken);
        }catch (Exception e){
            restResponse = new RESTResponse.CustomError()
                    .setMessage("Fail !")
                    .setCode(HttpStatus.FAILED_DEPENDENCY.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }

        Invoice newInvoice = new Invoice("Up vote meme by token !","Up vote !",amountOfToken,user);
        InvoiceDTO newInvoiceDTO = new InvoiceDTO(newInvoice);
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(newInvoiceDTO).build();
        return ResponseEntity.ok().body(restResponse);
    }

}
