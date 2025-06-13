package com.juahaki.juahaki.service.customauth;

import com.juahaki.juahaki.dto.auth.JwtResponse;
import com.juahaki.juahaki.dto.auth.LoginRequest;
import com.juahaki.juahaki.dto.auth.RefreshTokenRequest;
import com.juahaki.juahaki.dto.auth.SignUpRequest;
import com.juahaki.juahaki.dto.otp.VerifyOtpRequest;
import com.juahaki.juahaki.dto.user.UserInfo;
import com.juahaki.juahaki.exception.AlreadyExistsException;
import com.juahaki.juahaki.exception.CustomException;
import com.juahaki.juahaki.mapper.UserMapper;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.service.email.IEmailService;
import com.juahaki.juahaki.service.otp.IOtpService;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtHelperService jwtHelperService;
    private final IOtpService otpService;
    private final IEmailService emailService;
    private final UserMapper userMapper;

    @Override
    public UserInfo signUp(SignUpRequest signUpRequest) {
        validateSignUpRequest(signUpRequest);
        return Optional.of(signUpRequest)
                .filter(request -> !userRepository.existsByUsername(request.getUsername()))
                .filter(request -> !userRepository.existsByEmail(request.getEmail()))
                .map(this::createUser)
                .map(userRepository::save)
                .map(user -> {
                    sendWelcomeEmail(user);
                    sendVerificationOtp(user);
                    return user;
                })
                .map(userMapper::mapToUserInfo)
                .orElseThrow(() -> handleSignUpException(signUpRequest));
    }

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        validateLoginRequest(loginRequest);

        return Optional.of(loginRequest)
                .map(this::authenticateUser)
                .map(auth -> (User) auth.getPrincipal())
                .map(this::validateUserAccount)
                .map(this::createJwtResponse)
                .orElseThrow(() -> new CustomException("Authentication failed"));
    }

    @Override
    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        validateRefreshTokenRequest(refreshTokenRequest);

        try {
            String refreshToken = refreshTokenRequest.getRefreshToken();
            Long userId = jwtHelperService.getCurrentUserIdFromToken(refreshToken);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException("User not found"));

            String[] tokens = jwtHelperService.refreshTokenPair(refreshToken, user);

            return JwtResponse.builder()
                    .accessToken(tokens[0])
                    .refreshToken(tokens[1])
                    .user(userMapper.mapToUserInfo(user))
                    .build();
        } catch (Exception e) {
            throw new CustomException("Invalid or expired refresh token");
        }
    }

    @Override
    public void verifyEmailOtp(VerifyOtpRequest verifyOtpRequest) {
        validateVerifyOtpRequest(verifyOtpRequest);

        try {
            otpService.activateUserAccount(verifyOtpRequest.getEmail(), verifyOtpRequest.getOtp());
        } catch (Exception e) {
            throw new CustomException("Email verification failed: " + e.getMessage());
        }
    }

    @Override
    public void resendEmailVerificationOtp(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }

        try {
            otpService.resendEmailVerificationOtp(email);
        } catch (Exception e) {
            throw new CustomException("Failed to resend verification email: " + e.getMessage());
        }
    }

    @Override
    public void validateSignUpRequest(SignUpRequest request) {
        Optional.ofNullable(request)
                .filter(r -> StringUtils.hasText(r.getUsername()))
                .filter(r -> StringUtils.hasText(r.getEmail()))
                .filter(r -> StringUtils.hasText(r.getPassword()))
                .filter(r -> StringUtils.hasText(r.getFirstName()))
                .filter(r -> StringUtils.hasText(r.getLastName()))
                .orElseThrow(() -> new IllegalArgumentException("All fields are required"));
    }

    @Override
    public void validateLoginRequest(LoginRequest request) {
        Optional.ofNullable(request)
                .filter(r -> StringUtils.hasText(r.getUsernameOrEmail()))
                .filter(r -> StringUtils.hasText(r.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("Username/email and email are required"));
    }

    @Override
    public void validateRefreshTokenRequest(RefreshTokenRequest request) {
        Optional.ofNullable(request)
                .filter(r -> StringUtils.hasText(r.getRefreshToken()))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is required"));
    }

    @Override
    public void validateVerifyOtpRequest(VerifyOtpRequest request) {
        Optional.ofNullable(request)
                .filter(r -> StringUtils.hasText(r.getEmail()))
                .filter(r -> StringUtils.hasText(r.getOtp()))
                .orElseThrow(() -> new IllegalArgumentException("Email and Otp are required"));
    }

    @Override
    public User createUser(SignUpRequest request) {
        try {
            return User.builder()
                    .firstName(request.getFirstName().trim())
                    .lastName(request.getLastName().trim())
                    .username(request.getUsername().trim().toLowerCase())
                    .email(request.getEmail().trim().toLowerCase())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .isEnabled(false)
                    .emailVerified(false)
                    .build();
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyExistsException("Username or Email already exists");
        }
    }

    @Override
    public RuntimeException handleSignUpException(SignUpRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return new CustomException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return new CustomException("Email already exists");
        }
        return new CustomException("Registration failed");
    }

    @Override
    public Authentication authenticateUser(LoginRequest request) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new CustomException("Invalid username/email or password");
        }
    }

    @Override
    public JwtResponse createJwtResponse(User user) {
        try {
            String[] tokens = jwtHelperService.generateTokenPair(user);

            UserInfo userInfo = userMapper.mapToUserInfo(user);

            return JwtResponse.builder()
                    .accessToken(tokens[0])
                    .refreshToken(tokens[1])
                    .user(userMapper.mapToUserInfo(user))
                    .build();
        } catch (Exception e) {
            throw new CustomException("Failed to generate authentication tokens");
        }
    }

    private User validateUserAccount(User user) {
        if (!user.isEnabled()) {
            throw new CustomException("Account is disabled. Please verify your email first");
        }

        if (!user.getEmailVerified()) {
            throw new CustomException("Email not verified. Please check your email for verification");
        }

        return user;
    }

    private void sendWelcomeEmail(User user) {
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.warn("Failed to send welcome email to user: {}", user.getEmail());
        }
    }

    private void sendVerificationOtp(User user) {
        try {
            otpService.generateAndSendEmailVerificationOtp(user);
        } catch (Exception e) {
            log.error("Failed to send email verification OTP to user");
        }
    }
}
