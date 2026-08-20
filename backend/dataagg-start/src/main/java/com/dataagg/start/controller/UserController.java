package com.dataagg.start.controller;

import com.dataagg.common.dto.LoginResponse;
import com.dataagg.common.entity.User;
import com.dataagg.common.result.Result;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.cache.MultiLevelCache;
import com.dataagg.service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private MultiLevelCache cache;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @RequireAuth
    public Result<LoginResponse> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LoginResponse resp = cache.get("users", String.valueOf(userId), LoginResponse.class, () -> {
            User user = userService.getById(userId);
            if (user == null) return null;
            LoginResponse r = new LoginResponse();
            r.setUserId(user.getId());
            r.setUsername(user.getUsername());
            r.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
            r.setAvatar(user.getAvatar());
            r.setEmail(user.getEmail());
            r.setRole(user.getRole());
            return r;
        });
        if (resp == null) {
            return Result.error("用户不存在");
        }
        return Result.success(resp);
    }

    @PutMapping("/change-password")
    @RequireAuth
    public Result<Void> changePassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || oldPassword.isBlank()) return Result.error("请输入旧密码");
        if (newPassword == null || newPassword.isBlank()) return Result.error("请输入新密码");
        if (newPassword.length() < 6) return Result.error("新密码至少6位");

        User user = userService.getById(userId);
        if (user == null) return Result.error("用户不存在");
        if (user.getPassword() == null) return Result.error("当前账号未设置密码");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码不正确");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateById(user);
        cache.evict("users", String.valueOf(userId));
        return Result.success();
    }
}
