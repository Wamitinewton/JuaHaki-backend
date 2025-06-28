package com.juahaki.juahaki.service.oauth;

import com.juahaki.juahaki.enums.AuthProvider;
import com.juahaki.juahaki.exception.CustomException;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.security.oauth.OAuth2UserPrincipal;
import com.juahaki.juahaki.service.email.IEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final IEmailService emailService;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        userRequest.getClientRegistration().getRegistrationId();

        OidcUser oidcUser = super.loadUser(userRequest);

        try {
            return processOidcUser(userRequest, oidcUser);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException("OIDC authentication failed: " + ex.getMessage());
        }
    }

    private OidcUser processOidcUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String email = oidcUser.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new CustomException("Email not found from OIDC provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        boolean isNewUser = false;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            AuthProvider requestProvider = AuthProvider.valueOf(registrationId.toUpperCase());

            if (!user.getProvider().equals(requestProvider)) {
                // If user has LOCAL provider (email/password), allow OAuth2 login and update
                // provider
                if (user.getProvider() == AuthProvider.LOCAL) {
                    user.setProvider(requestProvider);
                    user.setProviderId(oidcUser.getSubject());
                    user = updateExistingUser(user, oidcUser);
                } else {
                    // User has a different OAuth2 provider
                    throw new CustomException("You're signed up with " + user.getProvider() +
                            " account. Please use your " + user.getProvider() + " account to login.");
                }
            } else {
                user = updateExistingUser(user, oidcUser);
            }
        } else {
            user = registerNewUser(userRequest, oidcUser);
            isNewUser = true;
        }

        if (isNewUser) {
            sendWelcomeEmail(user);
        }

        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(
                user,
                oidcUser.getAttributes(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo());

        return principal;
    }

    private User registerNewUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        User user = new User();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        user.setProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
        user.setProviderId(oidcUser.getSubject());
        user.setFirstName(oidcUser.getGivenName() != null ? oidcUser.getGivenName() : "");
        user.setLastName(oidcUser.getFamilyName() != null ? oidcUser.getFamilyName() : "");
        user.setEmail(oidcUser.getEmail());
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setImageUrl(oidcUser.getPicture());

        user.setUsername(generateUniqueUsername(oidcUser.getEmail()));

        user.setPassword(null);

        User savedUser = userRepository.save(user);
        return savedUser;
    }

    private User updateExistingUser(User existingUser, OidcUser oidcUser) {
        boolean updated = false;

        if (StringUtils.hasText(oidcUser.getGivenName()) &&
                !oidcUser.getGivenName().equals(existingUser.getFirstName())) {
            existingUser.setFirstName(oidcUser.getGivenName());
            updated = true;
        }

        if (StringUtils.hasText(oidcUser.getFamilyName()) &&
                !oidcUser.getFamilyName().equals(existingUser.getLastName())) {
            existingUser.setLastName(oidcUser.getFamilyName());
            updated = true;
        }

        if (StringUtils.hasText(oidcUser.getPicture()) &&
                !oidcUser.getPicture().equals(existingUser.getImageUrl())) {
            existingUser.setImageUrl(oidcUser.getPicture());
            updated = true;
        }

        if (!existingUser.getEmailVerified()) {
            existingUser.setEmailVerified(true);
            updated = true;
        }

        if (updated) {
            User savedUser = userRepository.save(existingUser);
            return savedUser;
        }

        return existingUser;
    }

    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        String username = baseUsername;

        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    private void sendWelcomeEmail(User user) {
        try {
            String firstName = StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : "Valued Customer";
            emailService.sendWelcomeEmail(user.getEmail(), firstName);
        } catch (Exception e) {

        }
    }
}
