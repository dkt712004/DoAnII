package com.dkt.authenticationservice.service;

import com.dkt.authenticationservice.dto.UserProfileDto;
import com.dkt.authenticationservice.entity.UserEntity;
import com.dkt.authenticationservice.entity.UserProfile;
import com.dkt.authenticationservice.repository.UserProfileRepository;
import com.dkt.authenticationservice.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserEntityRepository userEntityRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileDto getUserProfileByUsername(String username) {
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        UserProfile userProfile = userProfileRepository.findById(user.getId())
                .orElse(new UserProfile());

        return mapToDto(user, userProfile);
    }

    private UserProfileDto mapToDto(UserEntity user, UserProfile profile) {
        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setAvatarUrl(profile.getAvatarUrl());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setAddress(profile.getAddress());
        dto.setDateOfBirth(profile.getDateOfBirth());
        return dto;
    }
}