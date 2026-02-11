package com.sayai.record.fantasy.controller;

import com.sayai.record.fantasy.dto.PostDto;
import com.sayai.record.fantasy.entity.PostType;
import com.sayai.record.fantasy.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public List<PostDto> getPosts(@RequestParam(name = "type") PostType type) {
        return postService.getPostsByType(type);
    }
}
