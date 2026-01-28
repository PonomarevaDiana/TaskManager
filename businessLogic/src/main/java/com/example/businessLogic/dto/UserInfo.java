package com.example.businessLogic.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserInfo {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
}
