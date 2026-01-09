package com.dkt.authenticationservice.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class BlacklistStoreSc2 {

    // Đây là bộ nhớ cục bộ (RAM) dùng để chứa các token đã Logout
    private final Set<String> blacklist = Collections.synchronizedSet(new HashSet<>());

    public void addToBlacklist(String token) {
        blacklist.add(token);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }
}