package com.sayai.record.fantasy.repository;

import com.sayai.record.fantasy.entity.Post;
import com.sayai.record.fantasy.entity.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByTypeOrderByCreatedAtDesc(PostType type);
}
