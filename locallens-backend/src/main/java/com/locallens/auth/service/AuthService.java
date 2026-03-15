package com.locallens.auth.service;

import com.locallens.auth.dto.*;
import com.locallens.auth.model.UserDocument;
import com.locallens.auth.repository.UserRepository;
import com.locallens.common.exception.BadRequestException;
import com.locallens.common.exception.ResourceNotFoundException;
import com.locallens.common.exception.UnauthorizedException;
import com.locallens.common.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already registered");
        }

        UserDocument user = new UserDocument();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setRole(req.role());
        user.setAuthProvider("LOCAL");
        user.setCurrency("USD");
        user.setLanguage("en");
        user.setLastLoginAt(Instant.now());

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse authenticate(LoginRequest req) {
        UserDocument user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return buildAuthResponse(user);
    }

    public void logout(String userId) {
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setRefreshTokenHash(null);
        userRepository.save(user);
    }

    public UserDocument getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private AuthResponse buildAuthResponse(UserDocument user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        user.setRefreshTokenHash(passwordEncoder.encode(refreshToken));
        userRepository.save(user);
        return new AuthResponse(accessToken, refreshToken, UserProfileResponse.from(user));
    }
}
