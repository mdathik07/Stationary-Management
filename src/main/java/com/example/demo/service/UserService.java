package com.example.demo.service;

import com.example.demo.dto.UserRegistrationDto;
import com.example.demo.entity.User;

public interface UserService {

    /**
     * Registers a new STUDENT account.
     *
     * @throws com.example.demo.exception.RegistrationException if the email is
     *         already taken or outside the allowed college domain
     */
    User registerStudent(UserRegistrationDto dto);
}
