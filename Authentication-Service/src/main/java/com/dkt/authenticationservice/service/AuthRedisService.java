package com.dkt.authenticationservice.service;

import com.dkt.authenticationservice.dto.BaseResponse;
import com.dkt.authenticationservice.entity.UserEntity;
import com.dkt.authenticationservice.repository.UserEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRedisService {
    private final RedisCacheService redisCacheService;
    private final UserEntityRepository userEntityRepository;
    private final ObjectMapper objectMapper;


    public BaseResponse<String> login(String username, String password) {
        log.info("Login User {} ", username);
        BaseResponse<String> baseResponse = new BaseResponse<>();
        baseResponse.setCode("00");
        baseResponse.setMessage("Success");
        try {
            Optional<UserEntity> userEntityOpt = userEntityRepository.findByUsername(username);
            if (userEntityOpt.isEmpty()) {
                // TODO log
                baseResponse.setCode("01");
                baseResponse.setMessage("NOT FOUND USER");
                return baseResponse;
            }

            UserEntity userEntity = userEntityOpt.get();
            if (!password.equals(userEntity.getPassword())) {
                // TODO log
                baseResponse.setCode("02");
                baseResponse.setMessage("INCORRECT PASSWORD");
                return baseResponse;
            }

            String sessionId = redisCacheService.pushSession(userEntity);
            baseResponse.setData(sessionId);

        } catch (Exception e) {
            log.info("Error in login: {}", e.getMessage(), e);
            baseResponse.setCode("ERR");
            baseResponse.setMessage(e.getMessage());
        }
        return baseResponse;
    }


    public BaseResponse<?> logout(String sessionId) {
        BaseResponse<?> baseResponse = new BaseResponse<>();
        baseResponse.setCode("00");
        baseResponse.setMessage("Success");
        try {

            if (!redisCacheService.kickoutSession(sessionId)) {
                baseResponse.setCode("03");
                baseResponse.setMessage("INCORRECT SESSION");
                return baseResponse;
            }

        } catch (Exception e) {
            log.info("Error in logout: {}", e.getMessage(), e);
            baseResponse.setCode("ERR");
            baseResponse.setMessage(e.getMessage());
        }
        return baseResponse;
    }


    public BaseResponse<String> doSomeThing(String sessionId) {
        BaseResponse<String> baseResponse = new BaseResponse<>();
        baseResponse.setCode("00");
        baseResponse.setMessage("Success");
        try {
            UserEntity user = redisCacheService.getSession(sessionId);
            if (user == null) {
                baseResponse.setCode("01");
                baseResponse.setMessage("INCORRECT SESSION");
                return baseResponse;
            }

            log.info("USER: {}", objectMapper.writeValueAsString(user));

            baseResponse.setData(user.getUsername());
        } catch (Exception e) {
            log.info("Error in doSomeThing: {}", e.getMessage(), e);
            baseResponse.setCode("ERR");
            baseResponse.setMessage(e.getMessage());
        }

        return baseResponse;
    }
}
