package com.juahaki.juahaki.core.auth.service;

import com.juahaki.juahaki.core.auth.dto.JwtResponse;
import com.juahaki.juahaki.core.auth.dto.LoginRequest;
import com.juahaki.juahaki.core.auth.dto.RefreshTokenRequest;
import com.juahaki.juahaki.core.auth.dto.SignUpRequest;
import com.juahaki.juahaki.core.otp.dto.VerifyOtpRequest;
import com.juahaki.juahaki.core.user.dto.UserInfo;
import com.juahaki.juahaki.core.user.model.User;

public interface IAuthService {

    UserInfo signUp(SignUpRequest signUpRequest);

    JwtResponse login(LoginRequest loginRequest);

    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void verifyEmailOtp(VerifyOtpRequest verifyOtpRequest);

    void resendEmailVerificationOtp(String email);

    void validateSignUpRequest(SignUpRequest request);

    void validateLoginRequest(LoginRequest request);

    void validateRefreshTokenRequest(RefreshTokenRequest request);

    void validateVerifyOtpRequest(VerifyOtpRequest request);

    User createUser(SignUpRequest request);


    JwtResponse createJwtResponse(User user);
}