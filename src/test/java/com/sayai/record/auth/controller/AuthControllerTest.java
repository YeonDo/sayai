package com.sayai.record.auth.controller;

import com.sayai.record.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_shouldReturnToken() {
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("user");
        request.setPassword("pass");

        when(authService.login("user", "pass")).thenReturn("mock-token");

        ResponseEntity<AuthController.TokenResponse> response = authController.login(request);

        assertThat(response.getBody().getAccessToken()).isEqualTo("mock-token");
    }
}
