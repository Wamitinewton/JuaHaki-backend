package com.juahaki.juahaki.util.jwt;

import com.juahaki.juahaki.exception.CustomException;
import com.juahaki.juahaki.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtHelperService {

    private final JwtUtil jwtUtil;

    /**
     * Extract JWT token from Authorization header
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new CustomException("Authorization token is required");
    }

    /**
     * Get current user ID from request token
     */
    public Long getCurrentUserIdFromRequest(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return getCurrentUserIdFromToken(token);
    }

    /**
     * Get current user ID from token
     */
    public Long getCurrentUserIdFromToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                throw new CustomException("Invalid or expired token");
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                throw new CustomException("User ID not found in token");
            }
            return userId;
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            throw new CustomException("Failed to extract user information from token");
        }
    }

    /**
     * Get current username from request token
     */
    public String getCurrentUsernameFromRequest(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return getCurrentUsernameFromToken(token);
    }

    /**
     * Get current username from token
     */
    public String getCurrentUsernameFromToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                throw new CustomException("Invalid or expired token");
            }
            return jwtUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            throw new CustomException("Failed to extract username from token");
        }
    }

    /**
     * Get current user email from request token
     */
    public String getCurrentUserEmailFromRequest(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return getCurrentUserEmailFromToken(token);
    }

    /**
     * Get current user email from token
     */
    public String getCurrentUserEmailFromToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                throw new CustomException("Invalid or expired token");
            }
            return jwtUtil.getEmailFromToken(token);
        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            throw new CustomException("Failed to extract email from token");
        }
    }

    /**
     * Validate that the token belongs to the specified user ID
     */
    public boolean validateTokenOwnership(String token, Long expectedUserId) {
        try {
            Long tokenUserId = getCurrentUserIdFromToken(token);
            return tokenUserId.equals(expectedUserId);
        } catch (Exception e) {
            log.error("Error validating token ownership: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate both access and refresh tokens for a user
     * Returns array: [accessToken, refreshToken]
     */
    public String[] generateTokenPair(User user) {
        try {
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);
            return new String[] { accessToken, refreshToken };
        } catch (Exception e) {
            log.error("Error generating token pair for user {}: {}", user.getId(), e.getMessage());
            throw new CustomException("Failed to generate authentication tokens");
        }
    }


    public String refreshAccessToken(String refreshToken, User user) {
        try {
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new CustomException("Invalid or expired refresh token");
            }

            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new CustomException("Token is not a refresh token");
            }

            Long tokenUserId = jwtUtil.getUserIdFromToken(refreshToken);
            if (!user.getId().equals(tokenUserId)) {
                throw new CustomException("Refresh token does not belong to the user");
            }

            return jwtUtil.generateAccessToken(user);
        } catch (Exception e) {
            log.error("Error refreshing access token: {}", e.getMessage());
            throw new CustomException("Failed to refresh access token");
        }
    }


    public String[] refreshTokenPair(String refreshToken, User user) {
        try {
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new CustomException("Invalid or expired refresh token");
            }

            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new CustomException("Token is not a refresh token");
            }

            // Validate that the refresh token belongs to the user
            Long tokenUserId = jwtUtil.getUserIdFromToken(refreshToken);
            if (!user.getId().equals(tokenUserId)) {
                throw new CustomException("Refresh token does not belong to the user");
            }

            String newAccessToken = jwtUtil.generateAccessToken(user);
            String newRefreshToken = jwtUtil.generateRefreshToken(user);

            return new String[] { newAccessToken, newRefreshToken };
        } catch (Exception e) {
            log.error("Error refreshing token pair: {}", e.getMessage());
            throw new CustomException("Failed to refresh token pair");
        }
    }
}