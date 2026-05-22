package com.sayai.record.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.entity.OAuthProvider;
import com.sayai.record.auth.entity.UserSocialAccount;
import com.sayai.record.auth.jwt.JwtTokenProvider;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.auth.repository.UserSocialAccountRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final UserSocialAccountRepository socialAccountRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private final RestClient restClient = RestClient.create();

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Transactional(readOnly = true)
    public boolean getLinkStatus(Long memberId) {
        return socialAccountRepository.existsByProviderAndMemberMemberId(OAuthProvider.KAKAO, memberId);
    }

    @Transactional
    public void unlinkAccount(Long memberId) {
        UserSocialAccount social = socialAccountRepository
                .findByProviderAndMemberMemberId(OAuthProvider.KAKAO, memberId)
                .orElseThrow(() -> new IllegalStateException("연동된 카카오 계정이 없습니다."));
        socialAccountRepository.delete(social);
        log.info("[KAKAO] unlinked memberId={}", memberId);
    }

    @Transactional
    public void linkAccount(Long memberId, String code) {
        String accessToken = fetchAccessToken(code);
        KakaoUserInfo userInfo = fetchUserInfo(accessToken);
        String kakaoId = String.valueOf(userInfo.getId());

        socialAccountRepository.findByProviderAndProviderId(OAuthProvider.KAKAO, kakaoId).ifPresent(existing -> {
            if (!existing.getMember().getMemberId().equals(memberId)) {
                throw new IllegalStateException("이미 다른 계정에 연동된 카카오 계정입니다.");
            }
        });

        if (socialAccountRepository.existsByProviderAndMemberMemberId(OAuthProvider.KAKAO, memberId)) {
            return;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        socialAccountRepository.save(
                UserSocialAccount.builder()
                        .member(member)
                        .provider(OAuthProvider.KAKAO)
                        .providerId(kakaoId)
                        .build());

        log.info("[KAKAO] linked kakaoId={} to memberId={}", kakaoId, memberId);
    }

    public record LoginResult(String token, String name) {}

    @Transactional
    public LoginResult login(String code) {
        String accessToken = fetchAccessToken(code);
        KakaoUserInfo userInfo = fetchUserInfo(accessToken);
        String kakaoId = String.valueOf(userInfo.getId());

        UserSocialAccount social = socialAccountRepository
                .findByProviderAndProviderId(OAuthProvider.KAKAO, kakaoId)
                .orElseGet(() -> createMemberAndSocial(kakaoId, userInfo.getNickname()));

        Member member = social.getMember();
        String token = jwtTokenProvider.createToken(
                member.getMemberId(), member.getUserId(), member.getRole(), member.getName());
        return new LoginResult(token, member.getName());
    }

    private String fetchAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        KakaoTokenResponse tokenResponse = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        (req, res) -> { throw new IllegalStateException("Failed to get Kakao access token: " + res.getStatusCode()); })
                .body(KakaoTokenResponse.class);

        if (tokenResponse == null) {
            throw new IllegalStateException("Empty Kakao token response");
        }
        return tokenResponse.getAccessToken();
    }

    private KakaoUserInfo fetchUserInfo(String accessToken) {
        KakaoUserInfo userInfo = restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        (req, res) -> { throw new IllegalStateException("Failed to get Kakao user info: " + res.getStatusCode()); })
                .body(KakaoUserInfo.class);

        if (userInfo == null) {
            throw new IllegalStateException("Empty Kakao user info response");
        }
        return userInfo;
    }

    private UserSocialAccount createMemberAndSocial(String kakaoId, String nickname) {
        Long newMemberId = memberRepository.findTopByOrderByMemberIdDesc()
                .map(m -> m.getMemberId() + 1)
                .orElse(1L);

        Member member = Member.builder()
                .memberId(newMemberId)
                .userId("kakao_" + newMemberId)
                .name(nickname)
                .password(UUID.randomUUID().toString())
                .role(Member.Role.USER)
                .build();
        memberRepository.save(member);

        log.info("[KAKAO] new member created memberId={} userId={}", newMemberId, member.getUserId());

        return socialAccountRepository.save(
                UserSocialAccount.builder()
                        .member(member)
                        .provider(OAuthProvider.KAKAO)
                        .providerId(kakaoId)
                        .build());
    }

    @Data
    private static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
    }

    @Data
    private static class KakaoUserInfo {
        private Long id;
        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        public String getNickname() {
            if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
                return kakaoAccount.getProfile().getNickname();
            }
            return "카카오유저";
        }

        @Data
        private static class KakaoAccount {
            private KakaoProfile profile;
        }

        @Data
        private static class KakaoProfile {
            private String nickname;
        }
    }
}
