package com.sayai.record.auth.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerPasswordTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void changePassword_shouldCallServiceAndReturnOk() {
        Member member = mock(Member.class);
        when(member.getPlayerId()).thenReturn(100L);

        AuthController.ChangePasswordRequest request = new AuthController.ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass");

        ResponseEntity<String> response = authController.changePassword(member, request);

        verify(authService).changePassword(100L, "oldPass", "newPass");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Password changed successfully");
    }

    @Test
    void changePassword_shouldReturnUnauthorized_whenUserDetailsIsNull() {
        AuthController.ChangePasswordRequest request = new AuthController.ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass");

        ResponseEntity<String> response = authController.changePassword(null, request);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
    }
}
