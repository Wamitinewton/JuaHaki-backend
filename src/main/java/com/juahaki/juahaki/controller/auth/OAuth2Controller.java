package com.juahaki.juahaki.controller.auth;

import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.security.oauth.OAuth2UserPrincipal;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("${api.prefix}/auth/oauth2")
@RequiredArgsConstructor
@Validated
public class OAuth2Controller {

    private final JwtHelperService jwtHelperService;

    @Value("${app.oauth2.authorizedRedirectUri}")
    private String authorizedRedirectUri;

    @Value("${app.oauth2.webRedirectUri:#{null}}")
    private String webRedirectUri;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.redirect.uri}")
    private String googleRedirectUri;

    @GetMapping("/google")
    public void initiateGoogleAuth(
            @RequestParam(defaultValue = "android") String client,
            HttpServletResponse response) throws IOException {

        log.info("Initiating Google OAuth2 authentication for client: {}", client);

        String authUrl = "/oauth2/authorize/google";
        response.sendRedirect(authUrl);

        log.info("Redirected to Google OAuth2 authorization URL");
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

        log.info("Processing OAuth2 callback for provider: {}, client: {}", provider, client);

        if (error != null) {
            log.warn("OAuth2 authentication failed: {}", errorDescription);
            redirectWithError(response, "OAuth2 authentication failed: " + errorDescription, client);
            return;
        }

        String[] tokens = Optional.ofNullable(authentication)
                .filter(Authentication::isAuthenticated)
                .map(auth -> (OAuth2UserPrincipal) auth.getPrincipal())
                .map(OAuth2UserPrincipal::getUser)
                .map(this::generateTokensForUser)
                .orElseThrow(() -> new RuntimeException("Authentication failed"));

        log.info("Successfully processed OAuth2 callback for user: {}",
                ((OAuth2UserPrincipal) authentication.getPrincipal()).getUser().getUsername());

        redirectWithSuccess(response, tokens[0], tokens[1], client);
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        log.info("Retrieving current OAuth2 user information");

        Map<String, Object> userData = Optional.ofNullable(oauth2User)
                .map(OAuth2User::getAttributes)
                .map(this::createUserDataMap)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        log.info("Successfully retrieved user information for: {}", userData.get("email"));
        return ResponseEntity.ok(new ApiResponse("User information retrieved successfully", userData));
    }

    @PostMapping("/token")
    public ResponseEntity<ApiResponse> generateTokenFromOAuth2(
            @AuthenticationPrincipal OAuth2UserPrincipal oauth2UserPrincipal) {

        log.info("Generating tokens from OAuth2 authentication");

        Map<String, Object> tokenData = Optional.ofNullable(oauth2UserPrincipal)
                .map(OAuth2UserPrincipal::getUser)
                .map(user -> {
                    String[] tokens = generateTokensForUser(user);
                    return createTokenResponseData(tokens, user);
                })
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        log.info("Successfully generated tokens for user: {}", oauth2UserPrincipal.getUser().getUsername());
        return ResponseEntity.ok(new ApiResponse("Tokens generated successfully", tokenData));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse> getAuthStatus(Authentication authentication) {
        log.info("Retrieving authentication status");

        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();

        Map<String, Object> statusData = Map.of(
                "authenticated", isAuthenticated,
                "authType", isAuthenticated ? authentication.getClass().getSimpleName() : "none"
        );

        log.info("Authentication status: {}", isAuthenticated);
        return ResponseEntity.ok(new ApiResponse("Authentication status retrieved", statusData));
    }

    @GetMapping("/authorize-url")
    public ResponseEntity<ApiResponse> getAuthorizationUrl(
            @RequestParam(defaultValue = "google") String provider,
            @RequestParam @NotBlank String codeVerifier,
            @RequestParam @NotBlank String state) {

        log.info("Generating authorization URL for provider: {}", provider);

        String codeChallenge = generateCodeChallenge(codeVerifier);

        String authUrl = UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
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

        log.info("Successfully generated authorization URL for provider: {}", provider);
        return ResponseEntity.ok(new ApiResponse("Authorization URL generated", responseData));
    }



    private String[] generateTokensForUser(User user) {
        log.debug("Generating token pair for user: {}", user.getUsername());
        return jwtHelperService.generateTokenPair(user);
    }

    private Map<String, Object> createUserDataMap(Map<String, Object> attributes) {
        return Map.of(
                "id", attributes.get("sub"),
                "email", attributes.get("email"),
                "name", attributes.get("name"),
                "picture", attributes.get("picture"),
                "emailVerified", attributes.get("email_verified")
        );
    }

    private Map<String, Object> createTokenResponseData(String[] tokens, User user) {
        return Map.of(
                "accessToken", tokens[0],
                "refreshToken", tokens[1],
                "tokenType", "Bearer",
                "user", user
        );
    }

    private void redirectWithSuccess(HttpServletResponse response, String accessToken, String refreshToken, String client)
            throws IOException {

        String redirectUri = determineRedirectUri(client);
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                .queryParam("success", "true")
                .build()
                .toUriString();

        log.info("Redirecting to success URL for client: {}", client);
        response.sendRedirect(targetUrl);
    }

    private void redirectWithError(HttpServletResponse response, String errorMessage, String client) throws IOException {
        String redirectUri = determineRedirectUri(client);
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .queryParam("success", "false")
                .build()
                .toUriString();

        log.warn("Redirecting to error URL for client: {} with error: {}", client, errorMessage);
        response.sendRedirect(targetUrl);
    }

    private String determineRedirectUri(String client) {
        if ("android".equals(client)) {
            return authorizedRedirectUri;
        }
        return webRedirectUri != null ? webRedirectUri : authorizedRedirectUri;
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}