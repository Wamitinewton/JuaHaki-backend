package com.juahaki.juahaki.core.auth.model.oauth;

import com.juahaki.juahaki.shared.enums.AuthProvider;
import com.juahaki.juahaki.shared.exception.CustomException;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.GOOGLE.toString())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else {
            throw new CustomException("Login with " + registrationId + " is not supported");
        }
    }

}
