package com.cinemax.cinemax.infrastructure.security;

import com.cinemax.cinemax.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final GoogleOAuth2ServiceHelper oAuth2ServiceHelper;

    public CustomOAuth2UserService(GoogleOAuth2ServiceHelper oAuth2ServiceHelper) {
        this.oAuth2ServiceHelper = oAuth2ServiceHelper;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        User user = oAuth2ServiceHelper.processUser(oAuth2User.getAttributes());

        // Pastikan principal hasil login membawa role dari database
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(user.getRole()));

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "email");
    }
}

