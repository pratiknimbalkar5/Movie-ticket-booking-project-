package com.moviebooking.service;

import com.moviebooking.dto.request.LoginRequest;
import com.moviebooking.dto.request.RegisterRequest;
import com.moviebooking.dto.response.AuthResponse;
import com.moviebooking.entity.User;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.security.JwtUtil;
import com.moviebooking.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanUsername = (request.getUsername() != null) ? request.getUsername().trim() : "";
        String cleanEmail = (request.getEmail() != null) ? request.getEmail().trim().toLowerCase() : "";

        if (cleanUsername.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (cleanEmail.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (userRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .username(cleanUsername)
                .email(cleanEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String token = jwtUtil.generateToken(userDetails, savedUser.getId(), savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String rawIdentifier = (request.getUsername() != null) ? request.getUsername().trim() : "";
        if (rawIdentifier.isBlank()) {
            throw new IllegalArgumentException("Username or email cannot be blank");
        }

        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(rawIdentifier, rawIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials or user not found"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
