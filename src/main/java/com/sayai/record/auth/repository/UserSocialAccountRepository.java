package com.sayai.record.auth.repository;

import com.sayai.record.auth.entity.OAuthProvider;
import com.sayai.record.auth.entity.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    boolean existsByProviderAndMemberMemberId(OAuthProvider provider, Long memberId);

    Optional<UserSocialAccount> findByProviderAndMemberMemberId(OAuthProvider provider, Long memberId);
}
