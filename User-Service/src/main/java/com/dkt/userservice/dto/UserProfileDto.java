package com.dkt.userservice.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileDto {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
}