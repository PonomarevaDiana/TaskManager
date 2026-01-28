package org.example.authapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class UserInfo {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
}
