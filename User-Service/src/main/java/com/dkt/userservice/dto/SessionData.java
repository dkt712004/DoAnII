package com.dkt.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionData implements Serializable {
    private String sessionId;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private List<String> roles;
    private long createdAt;
}