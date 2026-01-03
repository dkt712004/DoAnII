package com.dkt.authenticationservice.service;

import com.dkt.authenticationservice.entity.UserEntity;
import com.dkt.authenticationservice.repository.UserEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Method for save session of user
     * @param userEntity session data
     * @return sessionId
     */
    public String pushSession(UserEntity userEntity) {
        try {
            String sessionId = UUID.randomUUID().toString().replaceAll("-", "");
            redisTemplate.opsForValue().set(sessionId, objectMapper.writeValueAsString(userEntity));
            return sessionId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public UserEntity getSession(String sessionId) {
        try {
            String userEntity = (String) redisTemplate.opsForValue().get(sessionId);
            return objectMapper.readValue(userEntity, UserEntity.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean kickoutSession(String sessionId) {
        boolean result = false;
        try {
            if (redisTemplate.hasKey(sessionId)) {
                redisTemplate.delete(sessionId);
                result = true;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
