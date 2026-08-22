package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.security;

import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The only place in the user module that touches Spring Security's
 * UserDetails — the domain User stays a plain POJO (see User.java) and is
 * translated here on demand instead of implementing UserDetails directly,
 * unlike the reference implementation this module is based on.
 */
@Component
public class SpringUserDetailsAdapter implements UserDetailsService {

    private final UserRepository userRepository;

    public SpringUserDetailsAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.username())
                .password(user.passwordHash())
                .disabled(!user.enabled())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())))
                .build();
    }
}
