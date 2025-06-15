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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new AlreadyExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new AlreadyExistsException("Email already exists");
        }

        try {
            User user = createUser(signUpRequest);
            User savedUser = userRepository.save(user);
            
            sendWelcomeEmail(savedUser);
            sendVerificationOtp(savedUser);
            
            return userMapper.mapToUserInfo(savedUser);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation during user registration", e);
            throw new AlreadyExistsException("Username or email already exists");
        } catch (Exception e) {
            log.error("Unexpected error during user registration", e);
            throw new CustomException("Registration failed. Please try again");
        }
    }

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        validateLoginRequest(loginRequest);
        
        try {
            User user = findUserByUsernameOrEmail(loginRequest.getUsernameOrEmail());
            
            validateUserAccountStatus(user);
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );
            
            User authenticatedUser = (User) authentication.getPrincipal();
            return createJwtResponse(authenticatedUser);
            
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", loginRequest.getUsernameOrEmail());
            throw new BadCredentialsException("Invalid username/email or password");
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            throw new CustomException("Login failed. Please try again");
        }
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
            log.warn("Failed to refresh token", e);
            throw new CustomException("Invalid or expired refresh token");
        }
    }

    @Override
    public void verifyEmailOtp(VerifyOtpRequest verifyOtpRequest) {
        validateVerifyOtpRequest(verifyOtpRequest);

        try {
            otpService.activateUserAccount(verifyOtpRequest.getEmail(), verifyOtpRequest.getOtp());
        } catch (Exception e) {
            log.warn("Email verification failed for: {}", verifyOtpRequest.getEmail(), e);
            throw new CustomException("Email verification failed. Please check your OTP and try again");
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
            log.warn("Failed to resend verification email to: {}", email, e);
            throw new CustomException("Failed to resend verification email. Please try again");
        }
    }

    @Override
    public void validateSignUpRequest(SignUpRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration data is required");
        }
        
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("Username is required");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }
        if (!StringUtils.hasText(request.getFirstName())) {
            throw new IllegalArgumentException("First name is required");
        }
        if (!StringUtils.hasText(request.getLastName())) {
            throw new IllegalArgumentException("Last name is required");
        }
    }

    @Override
    public void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login data is required");
        }
        
        if (!StringUtils.hasText(request.getUsernameOrEmail())) {
            throw new IllegalArgumentException("Username or email is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    @Override
    public void validateRefreshTokenRequest(RefreshTokenRequest request) {
        if (request == null || !StringUtils.hasText(request.getRefreshToken())) {
            throw new IllegalArgumentException("Refresh token is required");
        }
    }

    @Override
    public void validateVerifyOtpRequest(VerifyOtpRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("OTP verification data is required");
        }
        
        if (!StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!StringUtils.hasText(request.getOtp())) {
            throw new IllegalArgumentException("OTP is required");
        }
    }

    @Override
    public User createUser(SignUpRequest request) {
        return User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .username(request.getUsername().trim().toLowerCase())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .isEnabled(false)
                .emailVerified(false)
                .build();
    }



    @Override
    public JwtResponse createJwtResponse(User user) {
        try {
            String[] tokens = jwtHelperService.generateTokenPair(user);

            return JwtResponse.builder()
                    .accessToken(tokens[0])
                    .refreshToken(tokens[1])
                    .user(userMapper.mapToUserInfo(user))
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate JWT tokens for user: {}", user.getUsername(), e);
            throw new CustomException("Failed to generate authentication tokens");
        }
    }

    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid username/email or password"));
    }

    private void validateUserAccountStatus(User user) {
        if (!user.isEnabled()) {
            throw new CustomException("Account is disabled. Please verify your email to activate your account");
        }

        if (!user.getEmailVerified()) {
            throw new CustomException("Email not verified. Please check your email for the verification code");
        }
    }

    private void sendWelcomeEmail(User user) {
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.warn("Failed to send welcome email to user: {}", user.getEmail(), e);
        }
    }

    private void sendVerificationOtp(User user) {
        try {
            otpService.generateAndSendEmailVerificationOtp(user);
        } catch (Exception e) {
            log.error("Failed to send email verification OTP to user: {}", user.getEmail(), e);
        }
    }
}