package com.t1908e.memeportalapi.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.service.UserService;
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
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {
    private final UserService userService;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getUserDetail(@PathVariable(name = "id") long id) {
        return userService.getUserDetail(id);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateProfile(
            @PathVariable(name = "id") long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody @Valid UserDTO.UpdateUserProfileDTO userProfileDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Update user failed");
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
        return userService.updateUser(id, userProfileDTO, username);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deActiveUser(@PathVariable(name = "id") long id) {
        return userService.deActiveUser(id);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<?> searchUsers(
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "gender", required = false) Integer gender,
            @RequestParam(name = "fullName", required = false) String fullName,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy
    ) {
        HashMap<String, Object> params = new HashMap<>();
        if (status != null) {
            params.put("status", status);
        }
        if (gender != null) {
            params.put("gender", gender);
        }
        if (fullName != null) {
            params.put("fullName", fullName);
        }
        if (role != null) {
            params.put("role", role);
        }
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "id";
        return userService.searchListUsers(params, page - 1, limit, sortBy, order);
    }

    @RequestMapping(value = "/topToken", method = RequestMethod.GET)
    public ResponseEntity<?> getTopTokenOwner() {
        return userService.getTopTokenOwner();
    }

    @RequestMapping(value = "/{id}/postCreated", method = RequestMethod.GET)
    public ResponseEntity<?> getPostCreated(@PathVariable(value = "id") long id) {
        return userService.getPostCreated(id);
    }

    @RequestMapping(value = "/{id}/commentCount", method = RequestMethod.GET)
    public ResponseEntity<?> getTotalComment(@PathVariable(value = "id") long id) {
        return userService.getTotalComment(id);
    }
}
