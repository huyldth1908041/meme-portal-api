package com.t1908e.memeportalapi.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.t1908e.memeportalapi.dto.AdvertisementDTO;
import com.t1908e.memeportalapi.service.AdvertisementService;
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
@RequestMapping("/api/v1/advertisements")
@RequiredArgsConstructor
@CrossOrigin
public class AdvertisementController {
    private final AdvertisementService advertisementService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> createAdvertisement(
            @RequestBody @Valid AdvertisementDTO.CreateAdvertisementDTO dto,
            BindingResult bindingResult,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    ) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new RESTResponse
                    .CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Required token in header")
                    .build());
        }
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "create ads failed");
        }
        String accessToken = token.replace("Bearer", "").trim();
        DecodedJWT decodedJWT = JwtUtil.getDecodedJwt(accessToken);
        String username = decodedJWT.getSubject();
        return advertisementService.createAdvertisement(dto, username);
    }
    @RequestMapping(value = "/verify", method = RequestMethod.POST)
    public ResponseEntity<?> verifyAds(@RequestBody @Valid AdvertisementDTO.VerifyAdsDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "create ads failed");
        }
        return advertisementService.verifyAdvertisements(dto.getListIds());
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteAdvert(@PathVariable(value = "id") int id) {
        return advertisementService.deleteAd(id);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getDetail(@PathVariable(value = "id") int id) {
        return advertisementService.getAdvertisementDetail(id);
    }

    @RequestMapping(value = "/active", method = RequestMethod.GET)
    public ResponseEntity<?> getActiveAd() {
        return advertisementService.getActiveAdvertisement();
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<?> searchAds(
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy
    ) {
        HashMap<String, Object> params = new HashMap<>();
        if (status != null) {
            params.put("status", status);
        }
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "id";
        return advertisementService.searchListAdvertisement(params, page - 1, limit, sortBy, order);
    }
}
