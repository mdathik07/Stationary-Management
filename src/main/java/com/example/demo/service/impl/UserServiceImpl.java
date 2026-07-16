package com.example.demo.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserRegistrationDto;
import com.example.demo.entity.User;
import com.example.demo.exception.RegistrationException;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** e.g. "mycollege.edu"; empty means any domain is accepted. */
    @Value("${app.allowed-email-domain:}")
    private String allowedEmailDomain;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerStudent(UserRegistrationDto dto) {
        String email = dto.getEmail().trim().toLowerCase();

        if (!allowedEmailDomain.isBlank()
                && !email.endsWith("@" + allowedEmailDomain.toLowerCase())) {
            throw new RegistrationException("email",
                    "Please use your @" + allowedEmailDomain + " college email address.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("email", "This email is already registered. Try logging in.");
        }

        User user = User.builder()
                .name(dto.getName().trim())
                .email(email)
                .rollNumber(dto.getRollNumber().trim().toUpperCase())
                .contact(dto.getContact().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(User.Role.STUDENT)
                .build();
        return userRepository.save(user);
    }
}
