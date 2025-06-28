package com.juahaki.juahaki.service.customauth;

import com.juahaki.juahaki.dto.auth.JwtResponse;
import com.juahaki.juahaki.dto.auth.LoginRequest;
import com.juahaki.juahaki.dto.auth.RefreshTokenRequest;
import com.juahaki.juahaki.dto.auth.SignUpRequest;
import com.juahaki.juahaki.dto.otp.VerifyOtpRequest;
import com.juahaki.juahaki.dto.user.UserInfo;
import com.juahaki.juahaki.model.user.User;

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