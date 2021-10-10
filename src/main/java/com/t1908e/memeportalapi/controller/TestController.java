package com.t1908e.memeportalapi.controller;

import com.t1908e.memeportalapi.dto.PostDetailDTO;
import com.t1908e.memeportalapi.repository.CateroryRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
import com.t1908e.memeportalapi.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@CrossOrigin
public class TestController {

    private final PostRepository postRepository;
    private final PostService postService;

    private final CateroryRepository categoryRepository;

    private final    UserRepository userRepository;

    @RequestMapping(value = "/testList",method = RequestMethod.GET)
    public ResponseEntity<Iterable<PostDetailDTO>> testList(){

        Iterable<PostDetailDTO> posts  = postService.getListPost(PageRequest.of(0, 3));
        //        for ( Post cate: posts
//             ) {
//            System.out.print(cate.getId());
//
//        }
//        Map<String, Object> response = new HashMap<>();
//        response.put("posts", posts);

        return  new ResponseEntity<>( posts, new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/hello",method = RequestMethod.GET)
    public String test(){
        return "hello";
    }
}
