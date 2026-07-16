package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public registration always creates a STUDENT — the role is never taken
 * from the form. Admin accounts are seeded from configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDto {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name is too long")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Roll number is required")
    @Size(max = 30, message = "Roll number is too long")
    private String rollNumber;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "[0-9+\\-\\s]{7,15}", message = "Enter a valid contact number")
    private String contact;
}
