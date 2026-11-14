package com.cinemax.cinemax.infrastructure.security;

import com.cinemax.cinemax.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final GoogleOAuth2ServiceHelper oAuth2ServiceHelper;

    public CustomOidcUserService(GoogleOAuth2ServiceHelper oAuth2ServiceHelper) {
        this.oAuth2ServiceHelper = oAuth2ServiceHelper;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        User user = oAuth2ServiceHelper.processUser(oidcUser.getAttributes());

        // Pastikan principal hasil login membawa role dari database
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(user.getRole()));

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
    }
}

