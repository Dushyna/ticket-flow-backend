package io.github.dushyna.ticketflow.security.service;

import io.github.dushyna.ticketflow.user.entity.AppUser;
import io.github.dushyna.ticketflow.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String firstName = (String) attributes.get("given_name");
        String lastName = (String) attributes.get("family_name");
        String providerId = oAuth2User.getName();
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        userService.getByEmail(email.toLowerCase())
                .map(existingUser -> {
                    if (existingUser.getProviderId() == null) {
                        existingUser.setProvider(provider);
                        existingUser.setProviderId(providerId);
                        return userService.saveOrUpdate(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    AppUser newUser = new AppUser(email, firstName, lastName, provider, providerId);
                    return userService.saveOrUpdate(newUser);
                });

        return oAuth2User;
    }
}
