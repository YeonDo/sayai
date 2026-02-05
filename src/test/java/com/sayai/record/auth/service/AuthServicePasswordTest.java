package com.sayai.record.auth.service;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void changePassword_shouldThrowException_whenPasswordIsEmpty() {
        assertThatThrownBy(() -> authService.changePassword(1L, "", "newPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password cannot be empty");

        assertThatThrownBy(() -> authService.changePassword(1L, "oldPass", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password cannot be empty");
    }

    @Test
    void changePassword_shouldThrowException_whenNewPasswordIsInvalid() {
        assertThatThrownBy(() -> authService.changePassword(1L, "oldPass", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be at least 8 characters long and contain both letters and numbers");

        assertThatThrownBy(() -> authService.changePassword(1L, "oldPass", "onlyletters"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be at least 8 characters long and contain both letters and numbers");

        assertThatThrownBy(() -> authService.changePassword(1L, "oldPass", "12345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be at least 8 characters long and contain both letters and numbers");
    }

    @Test
    void changePassword_shouldSucceed_whenPasswordsAreValid() {
        Member member = Member.builder()
                .playerId(1L)
                .password("encodedOldPass")
                .role(Member.Role.USER)
                .build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass1")).thenReturn("encodedNewPass");

        authService.changePassword(1L, "oldPass", "newPass1");

        verify(passwordEncoder).encode("newPass1");
        // Verify member was updated (mocking Member might be tricky as it's an entity,
        // but verify interactions passed)
    }
}
