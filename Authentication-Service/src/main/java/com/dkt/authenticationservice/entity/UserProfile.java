package com.dkt.authenticationservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfile implements Serializable {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
}