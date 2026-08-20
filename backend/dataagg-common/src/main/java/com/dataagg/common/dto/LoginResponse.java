package com.dataagg.common.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String refreshToken;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String role;
}
