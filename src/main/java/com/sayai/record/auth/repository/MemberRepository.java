package com.sayai.record.auth.repository;

import com.sayai.record.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.CacheEvict;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUserId(String userId);

    boolean existsByPlayerIdOrUserId(Long playerId, String userId);

    @Cacheable(value = "members", key = "#id")
    Optional<Member> findById(Long id);

    @CachePut(value = "members", key = "#result.playerId")
    Member save(Member member);

    @CacheEvict(value = "members", key = "#id")
    void deleteById(Long id);

    @CacheEvict(value = "members", key = "#member.playerId")
    void delete(Member member);
}
