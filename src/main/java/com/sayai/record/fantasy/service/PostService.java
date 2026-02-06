package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.PostDto;
import com.sayai.record.fantasy.entity.Post;
import com.sayai.record.fantasy.entity.PostType;
import com.sayai.record.fantasy.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public PostDto createPost(PostDto dto) {
        Post post = Post.builder()
                .type(dto.getType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();
        Post saved = postRepository.save(post);
        return PostDto.from(saved);
    }

    public List<PostDto> getPostsByType(PostType type) {
        return postRepository.findByTypeOrderByCreatedAtDesc(type).stream()
                .map(PostDto::from)
                .collect(Collectors.toList());
    }
}
