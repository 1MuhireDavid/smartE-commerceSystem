package org.ecommerce.api.security;

import org.ecommerce.api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Accepts either username or email so both can be used as login identifiers. */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
    }
}
