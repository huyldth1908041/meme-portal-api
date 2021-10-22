package com.t1908e.memeportalapi.controller;

import com.t1908e.memeportalapi.service.PostService;
import com.t1908e.memeportalapi.service.UserService;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {
    @RequestMapping(value = "/testString", method = RequestMethod.GET)
    public String test(){
        return "ok";
    }
    private final UserService userService;
    private final PostService postService;

    @RequestMapping(value = "/newUsers", method = RequestMethod.GET)
    public ResponseEntity<?> getNewUsers(
            @RequestParam(name = "lastCreateAtDay", defaultValue = "2", required = false) Integer lastCreateAtDay

    ) {

        return userService.listNewUsers(lastCreateAtDay);
    }

    @RequestMapping(value = "/newPosts", method = RequestMethod.GET)
    public ResponseEntity<?> getNewPost(
            @RequestParam(name = "lastCreateAtDay",defaultValue = "2",required = false) Integer lastCreateAtDay)
    {

        return postService.listNewPosts(lastCreateAtDay);
    }




}
