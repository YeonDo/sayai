package com.sayai.record.admin.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminControllerPerfTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void testExistsAndFindPerformance() {
        Member member = Member.builder()
                .playerId(99999L)
                .userId("testuser999")
                .password("password")
                .name("Test User")
                .role(Member.Role.USER)
                .build();
        memberRepository.save(member);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (int i = 0; i < 1000; i++) {
            assertTrue(memberRepository.existsByPlayerIdOrUserId(99999L, "testuser999"));
        }

        stopWatch.stop();
        System.out.println("Time taken with combined query: " + stopWatch.getTotalTimeMillis() + " ms");
    }
}
