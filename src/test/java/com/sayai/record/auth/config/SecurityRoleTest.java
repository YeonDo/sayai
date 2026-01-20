package com.sayai.record.auth.config;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityRoleTest {

    @Test
    void role_shouldMapToAuthority() {
        Member.Role role = Member.Role.ADMIN;
        assertThat("ROLE_" + role.name()).isEqualTo("ROLE_ADMIN");
    }
}
