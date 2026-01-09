package com.dkt.authenticationservice.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class InMemoryTokenStore {
//    private final Set<String> validTokens = Collections.synchronizedSet(new HashSet<>());

    @Getter
    @Setter
    private List<String> listToken = new ArrayList<>();

    public void storeToken(String token) {
//        validTokens.add(token);

        getListToken().add(token);
    }

    public boolean isTokenValid(String token) {
        return getListToken().contains(token);
//        return validTokens.contains(token);
    }

//    public void invalidateToken(String token) {
////        validTokens.remove(token);
//        getListToken().remove(token);
//    }
    public boolean invalidateToken(String token) {
     // Hàm remove của List/Set trả về true nếu xóa thành công, false nếu không tìm thấy
        return getListToken().remove(token);
    }
}