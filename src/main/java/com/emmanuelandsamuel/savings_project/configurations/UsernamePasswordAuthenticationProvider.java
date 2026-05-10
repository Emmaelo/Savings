package com.emmanuelandsamuel.savings_project.configurations;

import com.emmanuelandsamuel.savings_project.entities.User;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
@Component
@RequiredArgsConstructor
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String email = authentication.getName();

        String password = Objects.requireNonNull(authentication.getCredentials()).toString();

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty())
            return null;

        User user = userOptional.get();

        if (!passwordEncoder.matches(password, user.getPassword()))
            return null;

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRole().name()));

        return new UsernamePasswordAuthenticationToken(user, null, authorities);

    }

    @Override
    public boolean supports(Class<?> authentication) {

        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);

    }
}
