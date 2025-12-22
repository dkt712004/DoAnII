package com.dkt.userservice.service;
import com.dkt.userservice.dto.UserProfileDto;
import com.dkt.userservice.entity.UserEntity;
import com.dkt.userservice.entity.UserProfile;
import com.dkt.userservice.repository.UserEntityRepository;
import com.dkt.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserEntityRepository userEntityRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfileByUsername(String username) {
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với username: " + username));

        UserProfile userProfile = userProfileRepository.findById(user.getId())
                .orElse(new UserProfile());

        return mapEntitiesToDto(user, userProfile);
    }

    private UserProfileDto mapEntitiesToDto(UserEntity user, UserProfile profile) {
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