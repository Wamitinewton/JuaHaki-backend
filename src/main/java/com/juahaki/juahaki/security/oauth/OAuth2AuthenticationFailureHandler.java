package com.juahaki.juahaki.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.authorizedRedirectUri}")
    private String redirectUri;

    @Value("${app.oauth2.webRedirectUri:#{null}}")
    private String webRedirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = extractErrorMessage(exception);
        String client = getClientType(request);
        String targetRedirectUri = determineRedirectUri(client);

        String targetUrl = UriComponentsBuilder.fromUriString(targetRedirectUri)
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .queryParam("success", "false")
                .build().toUriString();

        log.error("OAuth2 authentication failed for client type '{}': {}", client, errorMessage);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String extractErrorMessage(AuthenticationException exception) {
        String errorMessage = exception.getLocalizedMessage();
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            errorMessage = exception.getMessage();
        }
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            errorMessage = "Authentication failed";
        }
        return errorMessage;
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