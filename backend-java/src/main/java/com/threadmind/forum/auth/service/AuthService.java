package com.threadmind.forum.auth.service;

import com.threadmind.forum.auth.dto.AuthResponse;
import com.threadmind.forum.auth.dto.LoginRequest;
import com.threadmind.forum.auth.dto.RegisterRequest;
import com.threadmind.forum.auth.dto.UserSummary;
import com.threadmind.forum.auth.repository.UserRepository;
import com.threadmind.forum.auth.repository.UserRepository.UserRecord;
import com.threadmind.forum.common.security.CurrentUserProvider;
import com.threadmind.forum.common.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final JwtService jwtService;

    public AuthService(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            JwtService jwtService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        UserRecord user = userRepository.findActiveByLogin(request.username().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        try {
            Long userId = userRepository.createUser(request, passwordHash);
            UserRecord user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Created user is missing"));
            return buildAuthResponse(user);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists");
        }
    }

    public UserSummary currentUser() {
        UserRecord user = userRepository.findById(currentUserProvider.currentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user is missing"));
        return toSummary(user);
    }

    private AuthResponse buildAuthResponse(UserRecord user) {
        List<String> roles = userRepository.findRoleCodes(user.id());
        UserSummary summary = toSummary(user, roles);
        String token = jwtService.issueToken(user.id(), user.username(), roles);
        return new AuthResponse(token, summary);
    }

    private UserSummary toSummary(UserRecord user) {
        return toSummary(user, userRepository.findRoleCodes(user.id()));
    }

    private UserSummary toSummary(UserRecord user, List<String> roles) {
        return new UserSummary(
                user.id(),
                user.username(),
                user.nickname(),
                user.avatarUrl() == null ? "" : user.avatarUrl(),
                roles
        );
    }
}
