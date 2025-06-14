package com.juahaki.juahaki.controller.auth;

import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.security.oauth.OAuth2UserPrincipal;
import com.juahaki.juahaki.service.customauth.IAuthService;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("${api.prefix}/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final IAuthService authService;
    private final JwtHelperService jwtHelperService;

    @Value("${app.oauth2.authorizedRedirectUri}")
    private String authorizedRedirectUri; // For Android users

    @Value("${app.oauth2.webRedirectUri:#{null}}")
    private String webRedirectUri; // For Web Users

    @GetMapping("/google")
    public void initiateGoogleAuth(
            @RequestParam(defaultValue = "android") String client,
            HttpServletResponse response) throws IOException {
        String authUrl = "/oauth2/authorize/google";
        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback/{provider}")
    public void handleOAuth2Callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription,
            @RequestParam(defaultValue = "android") String client,
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        try {
            if (error != null) {
                redirectWithError(response, "OAuth2 authentication failed: " + errorDescription, client);
                return;
            }

            Optional.ofNullable(authentication)
                    .filter(Authentication::isAuthenticated)
                    .map(auth -> (OAuth2UserPrincipal) auth.getPrincipal())
                    .map(OAuth2UserPrincipal::getUser)
                    .map(this::generateTokensForUser)
                    .ifPresentOrElse(
                            tokens -> {
                                try {
                                    redirectWithSuccess(response, tokens[0], tokens[1], client);
                                } catch (IOException e) {
                                    log.error("Failed to redirect after successful authentication", e);
                                }
                            },
                            () -> {
                                try {
                                    redirectWithError(response, "Authentication failed", client);
                                } catch (IOException e) {
                                    log.error("Failed to redirect after authentication failure", e);
                                }
                            }
                    );

        } catch (Exception e) {
            log.error("Error processing OAuth2 callback", e);
            redirectWithError(response, "Authentication processing failed", client);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            return Optional.ofNullable(oauth2User)
                    .map(OAuth2User::getAttributes)
                    .map(this::createUserDataMap)
                    .map(userData -> ResponseEntity.ok(
                            new ApiResponse("User information retrieved successfully", userData)))
                    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ApiResponse("User not authenticated", null)));

        } catch (Exception e) {
            log.error("Failed to retrieve user information", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Failed to retrieve user information", null));
        }
    }

    @PostMapping("/token")
    public ResponseEntity<ApiResponse> generateTokenFromOAuth2(
            @AuthenticationPrincipal OAuth2UserPrincipal oauth2UserPrincipal) {
        try {
            return Optional.ofNullable(oauth2UserPrincipal)
                    .map(OAuth2UserPrincipal::getUser)
                    .map(this::generateTokensForUser)
                    .map(tokens -> createTokenResponseData(tokens, oauth2UserPrincipal.getUser()))
                    .map(tokenData -> ResponseEntity.ok(
                            new ApiResponse("Tokens generated successfully", tokenData)))
                    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ApiResponse("User not authenticated", null)));

        } catch (Exception e) {
            log.error("Failed to generate tokens", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Failed to generate tokens", null));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse> getAuthStatus(Authentication authentication) {
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();

        Map<String, Object> statusData = Map.of(
                "authenticated", isAuthenticated,
                "authType", isAuthenticated ? authentication.getClass().getSimpleName() : "none");

        return ResponseEntity.ok(new ApiResponse("Authentication status retrieved", statusData));
    }

    @GetMapping("/authorize-url")
    public ResponseEntity<ApiResponse> getAuthorizationUrl(
            @RequestParam(defaultValue = "google") String provider,
            @RequestParam String codeVerifier,
            @RequestParam String state) {
        try {
            String codeChallenge = generateCodeChallenge(codeVerifier);

            String authUrl = UriComponentsBuilder
                    .fromHttpUrl("https://accounts.google.com/o/oauth2/auth")
                    .queryParam("client_id", "${google.client.id}")
                    .queryParam("redirect_uri", "${google.redirect.uri}")
                    .queryParam("scope", "openid profile email")
                    .queryParam("response_type", "code")
                    .queryParam("state", state)
                    .queryParam("code_challenge", codeChallenge)
                    .queryParam("code_challenge_method", "S256")
                    .build()
                    .toUriString();

            Map<String, Object> responseData = Map.of(
                    "authorizationUrl", authUrl,
                    "state", state,
                    "codeChallenge", codeChallenge
            );

            return ResponseEntity.ok(new ApiResponse("Authorization URL generated", responseData));
        } catch (Exception e) {
            log.error("Failed to generate authorization URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Failed to generate authorization URL", null));
        }
    }

    private String[] generateTokensForUser(User user) {
        try {
            return jwtHelperService.generateTokenPair(user);
        } catch (Exception e) {
            log.error("Failed to generate tokens for user: {}", user.getId(), e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    private Map<String, Object> createUserDataMap(Map<String, Object> attributes) {
        return Map.of(
                "id", attributes.get("sub"),
                "email", attributes.get("email"),
                "name", attributes.get("name"),
                "picture", attributes.get("picture"),
                "emailVerified", attributes.get("email_verified"));
    }

    private Map<String, Object> createTokenResponseData(String[] tokens, User user) {
        return Map.of(
                "accessToken", tokens[0],
                "refreshToken", tokens[1],
                "tokenType", "Bearer",
                "user", user);
    }

    private void redirectWithSuccess(HttpServletResponse response, String accessToken, String refreshToken, String client)
            throws IOException {
        String targetUrl;

        if ("android".equals(client)) {
            targetUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                    .queryParam("token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                    .queryParam("success", "true")
                    .build().toUriString();
        } else {
            String redirectUri = webRedirectUri != null ? webRedirectUri : authorizedRedirectUri;
            targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                    .queryParam("success", "true")
                    .build().toUriString();
        }

        response.sendRedirect(targetUrl);
    }

    private void redirectWithError(HttpServletResponse response, String errorMessage, String client) throws IOException {
        String targetUrl;

        if ("android".equals(client)) {
            targetUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                    .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                    .queryParam("success", "false")
                    .build().toUriString();
        } else {
            String redirectUri = webRedirectUri != null ? webRedirectUri : authorizedRedirectUri;
            targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                    .queryParam("success", "false")
                    .build().toUriString();
        }

        response.sendRedirect(targetUrl);
    }

    // Helper method to generate code challenge for PKCE
    private String generateCodeChallenge(String codeVerifier) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }
}