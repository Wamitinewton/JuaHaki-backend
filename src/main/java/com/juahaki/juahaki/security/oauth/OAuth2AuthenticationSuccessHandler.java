package com.juahaki.juahaki.security.oauth;

import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.oauth2.authorizedRedirectUri}")
    private String redirectUri;

    @Value("${app.oauth2.webRedirectUri:#{null}}")
    private String webRedirectUri;

    private final JwtHelperService jwtHelperService;
    private final UserRepository userRepository;

    @Override
    protected String determineTargetUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        User user = extractUserFromAuthentication(authentication);

        if (user == null) {
            log.error("User extraction failed for authentication: {}",
                    authentication.getPrincipal().getClass().getSimpleName());
            return buildErrorUrl("User extraction failed", request);
        }

        try {
            String[] tokens = jwtHelperService.generateTokenPair(user);
            String accessToken = tokens[0];
            String refreshToken = tokens[1];

            return buildSuccessUrl(accessToken, refreshToken, request);
        } catch (Exception e) {
            log.error("Token generation failed for user: {}", user.getId(), e);
            return buildErrorUrl("Authentication Failed", request);
        }
    }

    private User extractUserFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2UserPrincipal) {
            return ((OAuth2UserPrincipal) principal).getUser();
        }

        if (principal instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) principal;
            String email = oidcUser.getEmail();

            if (email != null) {
                Optional<User> userOptional = userRepository.findByEmail(email);
                if (userOptional.isPresent()) {
                    return userOptional.get();
                } else {
                    log.warn("User not found in database for email");
                }
            } else {
                log.warn("Email not found in OIDC user attributes");
            }
        }

        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");

            if (email != null) {
                Optional<User> userOptional = userRepository.findByEmail(email);
                if (userOptional.isPresent()) {
                    return userOptional.get();
                } else {
                    log.warn("User not found in database for email: {}", email);
                }
            } else {
                log.warn("Email not found in OAuth2 user attributes");
            }
        }

        log.error("Unknown principal type: {}", principal.getClass().getSimpleName());
        return null;
    }

    private String buildSuccessUrl(String accessToken, String refreshToken, HttpServletRequest request) {
        String client = getClientType(request);
        String targetRedirectUri = determineRedirectUri(client);

        return UriComponentsBuilder.fromUriString(targetRedirectUri)
                .queryParam("token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                .queryParam("success", "true")
                .build().toUriString();
    }

    private String buildErrorUrl(String errorMessage, HttpServletRequest request) {
        String client = getClientType(request);
        String targetRedirectUri = determineRedirectUri(client);

        return UriComponentsBuilder.fromUriString(targetRedirectUri)
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .queryParam("success", "false")
                .build().toUriString();
    }

    private String getClientType(HttpServletRequest request) {
        String client = request.getParameter("client");
        String userAgent = request.getHeader("User-Agent");

        // Check if request comes from Android app
        if (client != null && client.equals("android")) {
            return "android";
        }

        // Check User-Agent for mobile indicators
        if (userAgent != null && (userAgent.contains("Android") || userAgent.contains("Mobile"))) {
            return "android";
        }

        return "web";
    }

    private String determineRedirectUri(String client) {
        if ("android".equals(client)) {
            return redirectUri;
        } else {
            return webRedirectUri != null ? webRedirectUri : redirectUri;
        }
    }
}