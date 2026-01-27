package com.sayai.record.auth.service;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.jwt.JwtTokenProvider;
import com.sayai.record.auth.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public String login(String userId, String password) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId or password"));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException("Invalid userId or password");
        }

        return jwtTokenProvider.createToken(member.getPlayerId(), member.getUserId(), member.getRole());
    }

    @Transactional(readOnly = true)
    public String getUserName(String userId) {
        return memberRepository.findByUserId(userId)
                .map(Member::getName)
                .orElse("Unknown");
    }

    @Transactional
    public void signup(String userId, String password, String name, Long playerId) {
        if (memberRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("User ID already exists");
        }

        Member member = Member.builder()
                .userId(userId)
                .password(passwordEncoder.encode(password))
                .name(name)
                .playerId(playerId)
                .role(Member.Role.USER) // Default role
                .build();

        memberRepository.save(member);
    }

    public Long getPlayerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return memberRepository.findByUserId(userDetails.getUsername())
                .map(Member::getPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
