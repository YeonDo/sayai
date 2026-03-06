package com.sayai.record.auth.service;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceBenchmarkTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setup() {
        memberRepository.deleteAll();
        if (cacheManager.getCache("members") != null) {
            cacheManager.getCache("members").clear();
        }
        Member member = Member.builder()
                .playerId(1L)
                .userId("testuser")
                .password(passwordEncoder.encode("oldPass123"))
                .name("Test User")
                .role(Member.Role.USER)
                .build();
        memberRepository.save(member);
    }

    @Test
    void benchmarkFindById() {
        // Warm up
        memberRepository.findById(1L);

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            memberRepository.findById(1L);
        }
        long duration = (System.nanoTime() - start) / 1_000_000; // ms
        System.out.println("Benchmark FindById Cache Active: " + duration + " ms");

        assertThat(memberRepository.findById(1L)).isPresent();
    }
}
