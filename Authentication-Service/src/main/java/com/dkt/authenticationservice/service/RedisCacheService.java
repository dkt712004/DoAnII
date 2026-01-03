package com.dkt.authenticationservice.service;

import com.dkt.authenticationservice.entity.UserEntity;
import com.dkt.authenticationservice.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserEntityRepository userEntityRepository;

    /**
     * Method for save session of user
     * @param userEntity session data
     * @return sessionId
     */
    public String pushSession(UserEntity userEntity) {
        try {
            String sessionId = UUID.randomUUID().toString().replaceAll("-", "");
            redisTemplate.opsForValue().set(sessionId, userEntity);
            return sessionId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public UserEntity getSession(String sessionId) {
        try {
            return (UserEntity) redisTemplate.opsForValue().get(sessionId);
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
