package com.t1908e.memeportalapi.controller;

import com.t1908e.memeportalapi.service.DashBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashBoardController {
    private final DashBoardService dashBoardService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getSystemsReport() {
        return dashBoardService.getDashboardReport();
    }

    @RequestMapping(value = "/postCount", method = RequestMethod.GET)
    public ResponseEntity<?> getPostCountByCreateTime(
            @RequestParam(name = "days", required = false) Integer days
    ) {
        if (days == null) days = 7;
        return dashBoardService.getPostCountByCreatedAt(days);
    }

    @RequestMapping(value = "/userCount", method = RequestMethod.GET)
    public ResponseEntity<?> getUserCountByCreateTime(
            @RequestParam(name = "days", required = false) Integer days
    ) {
        if (days == null) days = 7;
        return dashBoardService.getUserCountByCreatedAt(days);
    }
}
