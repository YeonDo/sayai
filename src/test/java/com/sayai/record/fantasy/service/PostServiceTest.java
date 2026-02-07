package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.PostDto;
import com.sayai.record.fantasy.entity.Post;
import com.sayai.record.fantasy.entity.PostType;
import com.sayai.record.fantasy.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void createPost_SavesAndReturnsDto() {
        PostDto input = PostDto.builder()
                .type(PostType.COLUMN)
                .title("New Column")
                .content("Content")
                .build();

        Post saved = Post.builder()
                .id(1L)
                .type(PostType.COLUMN)
                .title("New Column")
                .content("Content")
                .build();

        when(postRepository.save(any(Post.class))).thenReturn(saved);

        PostDto result = postService.createPost(input);

        assertEquals(1L, result.getId());
        assertEquals(PostType.COLUMN, result.getType());
        assertEquals("New Column", result.getTitle());
    }

    @Test
    void getPostsByType_ReturnsList() {
        Post post1 = Post.builder().id(1L).type(PostType.PATCH_NOTE).title("Patch 1").build();
        Post post2 = Post.builder().id(2L).type(PostType.PATCH_NOTE).title("Patch 2").build();

        when(postRepository.findByTypeOrderByCreatedAtDesc(PostType.PATCH_NOTE))
                .thenReturn(List.of(post1, post2));

        List<PostDto> result = postService.getPostsByType(PostType.PATCH_NOTE);

        assertEquals(2, result.size());
        assertEquals("Patch 1", result.get(0).getTitle());
    }
}
