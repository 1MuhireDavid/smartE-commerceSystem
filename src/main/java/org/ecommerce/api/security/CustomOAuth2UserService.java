package org.ecommerce.api.security;

import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the authenticated Google user, then finds or creates a matching
 * local UserEntity. The UserEntity is added to the OAuth2User attributes map
 * under the key "userEntity" so that OAuth2AuthenticationSuccessHandler can
 * retrieve it to generate a JWT without an extra DB lookup.
 *
 * Account-linking strategy:
 *   1. Try to find an existing user by (provider="google", providerId=sub).
 *   2. Fall back to email match — handles the case where the user previously
 *      registered with the same email using a local password.
 *   3. If neither matches, create a new account with role="customer".
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = (String) attributes.get("sub");
        String email      = (String) attributes.get("email");
        String name       = (String) attributes.getOrDefault("name", email);

        UserEntity user = resolveUser(providerId, email, name);

        // Attach the persisted entity so the success handler can build a JWT
        // without an additional DB round-trip.
        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("userEntity", user);

        return new DefaultOAuth2User(
            user.getAuthorities(),
            enrichedAttributes,
            "sub"  // the attribute that uniquely identifies the user
        );
    }

    private UserEntity resolveUser(String providerId, String email, String name) {
        // 1. Known Google account
        Optional<UserEntity> byProvider =
            userRepository.findByProviderAndProviderId("google", providerId);
        if (byProvider.isPresent()) {
            return byProvider.get();
        }

        // 2. Existing local account with the same email — link it
        Optional<UserEntity> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            UserEntity existing = byEmail.get();
            existing.setProvider("google");
            existing.setProviderId(providerId);
            return userRepository.save(existing);
        }

        // 3. Brand-new user — create an account
        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        newUser.setUsername(deriveUsername(email));
        newUser.setFullName(name);
        newUser.setPasswordHash(null);  // OAuth2 users have no local password
        newUser.setProvider("google");
        newUser.setProviderId(providerId);
        newUser.setRole("customer");
        newUser.setActive(true);
        return userRepository.save(newUser);
    }

    /**
     * Derives a username from the email's local part (before @).
     * Appends a numeric suffix if the name is already taken.
     */
    private String deriveUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        int suffix = 2;
        while (userRepository.existsByUsername(base + "_" + suffix)) {
            suffix++;
        }
        return base + "_" + suffix;
    }
}
