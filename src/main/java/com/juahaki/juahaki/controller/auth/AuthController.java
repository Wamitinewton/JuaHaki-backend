package com.juahaki.juahaki.controller.auth;

import com.juahaki.juahaki.dto.auth.JwtResponse;
import com.juahaki.juahaki.dto.auth.LoginRequest;
import com.juahaki.juahaki.dto.auth.RefreshTokenRequest;
import com.juahaki.juahaki.dto.auth.SignUpRequest;
import com.juahaki.juahaki.dto.otp.VerifyOtpRequest;
import com.juahaki.juahaki.dto.user.UserInfo;
import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.service.customauth.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("${api.prefix}/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signUp(@RequestBody SignUpRequest signUpRequest) {
        log.info("Processing signup request for email: {}", signUpRequest.getEmail());

        UserInfo userInfo = authService.signUp(signUpRequest);

        log.info("Successfully processed signup for user: {}", userInfo.getUsername());
        return ResponseEntity.ok(new ApiResponse("Successfully signed up. Please check your email for verification", userInfo));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("Processing login request for: {}", loginRequest.getUsernameOrEmail());

        JwtResponse jwtResponse = authService.login(loginRequest);

        log.info("Successfully processed login for user: {}", jwtResponse.getUser().getUsername());
        return ResponseEntity.ok(new ApiResponse("Successfully logged in", jwtResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.info("Processing token refresh request");

        JwtResponse jwtResponse = authService.refreshToken(refreshTokenRequest);

        log.info("Successfully refreshed token for user: {}", jwtResponse.getUser().getUsername());
        return ResponseEntity.ok(new ApiResponse("Successfully refreshed token", jwtResponse));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestBody VerifyOtpRequest verifyOtpRequest) {
        log.info("Processing email verification for: {}", verifyOtpRequest.getEmail());

        authService.verifyEmailOtp(verifyOtpRequest);

        log.info("Successfully verified email for: {}", verifyOtpRequest.getEmail());
        return ResponseEntity.ok(new ApiResponse("Email verified successfully! Your account is now active", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse> resendEmailVerification(@RequestParam String email) {
        log.info("Processing resend verification request for: {}", email);

        authService.resendEmailVerificationOtp(email);

        log.info("Successfully sent verification email to: {}", email);
        return ResponseEntity.ok(new ApiResponse("Verification email sent successfully! Please check your inbox.", null));
    }
}