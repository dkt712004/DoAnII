package com.dkt.authenticationservice.service;

import com.dkt.authenticationservice.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonService {
    private final RedisCacheService redisCacheService;

    public UserEntity getBySessionId(String sessionId) {
        UserEntity userEntity = redisCacheService.getSession(sessionId);
        if (Objects.isNull(userEntity)) {
            throw new RuntimeException("Not found Session Id:" + sessionId);
        }
        return  userEntity;
    }
}
