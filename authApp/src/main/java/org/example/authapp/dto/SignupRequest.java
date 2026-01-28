package org.example.authapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
}