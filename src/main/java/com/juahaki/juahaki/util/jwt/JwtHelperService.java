package com.juahaki.juahaki.util.jwt;

import com.juahaki.juahaki.enums.Role;
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
     * Get current user role from request token
     */
    public Role getCurrentUserRoleFromRequest(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return getCurrentUserRoleFromToken(token);
    }

    /**
     * Get current user role from token
     */
    public Role getCurrentUserRoleFromToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                throw new CustomException("Invalid or expired token");
            }
            Role role = jwtUtil.getRoleFromToken(token);
            if (role == null) {
                throw new CustomException("Role not found in token");
            }
            return role;
        } catch (Exception e) {
            log.error("Error extracting role from token: {}", e.getMessage());
            throw new CustomException("Failed to extract role from token");
        }
    }

    /**
     * Check if current user has specific role
     */
    public boolean hasRole(HttpServletRequest request, Role role) {
        try {
            String token = extractTokenFromRequest(request);
            return jwtUtil.hasRole(token, role);
        } catch (Exception e) {
            log.error("Error checking role: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user is admin
     */
    public boolean isAdmin(HttpServletRequest request) {
        return hasRole(request, Role.ADMIN);
    }

    /**
     * Check if current user is regular user
     */
    public boolean isUser(HttpServletRequest request) {
        return hasRole(request, Role.USER);
    }

    /**
     * Check if current user has admin role from token
     */
    public boolean isAdminFromToken(String token) {
        try {
            return jwtUtil.isAdmin(token);
        } catch (Exception e) {
            log.error("Error checking admin role from token: {}", e.getMessage());
            return false;
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
     * Validate that the token belongs to the specified user ID and has required role
     */
    public boolean validateTokenOwnershipAndRole(String token, Long expectedUserId, Role requiredRole) {
        try {
            Long tokenUserId = getCurrentUserIdFromToken(token);
            Role tokenRole = getCurrentUserRoleFromToken(token);
            return tokenUserId.equals(expectedUserId) && tokenRole.equals(requiredRole);
        } catch (Exception e) {
            log.error("Error validating token ownership and role: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if user can access resource (admin can access everything, user can access own resources)
     */
    public boolean canAccessResource(HttpServletRequest request, Long resourceUserId) {
        try {
            String token = extractTokenFromRequest(request);
            Long currentUserId = getCurrentUserIdFromToken(token);
            Role currentRole = getCurrentUserRoleFromToken(token);

            // Admin can access everything
            if (currentRole == Role.ADMIN) {
                return true;
            }

            // User can only access their own resources
            return currentUserId.equals(resourceUserId);
        } catch (Exception e) {
            log.error("Error checking resource access: {}", e.getMessage());
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