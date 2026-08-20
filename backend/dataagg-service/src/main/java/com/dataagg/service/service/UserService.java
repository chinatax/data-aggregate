package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.User;
import com.dataagg.service.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    public User getByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    public Map<String, String> getAvatarsByUsernames(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) return Map.of();
        return lambdaQuery()
                .select(User::getUsername, User::getAvatar)
                .in(User::getUsername, usernames)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getUsername, u -> u.getAvatar() != null ? u.getAvatar() : ""));
    }

    public User getByEmail(String email) {
        return lambdaQuery().eq(User::getEmail, email).one();
    }

    public User getInactiveByEmail(String email) {
        return lambdaQuery()
                .eq(User::getEmail, email)
                .eq(User::getStatus, "INACTIVE")
                .orderByDesc(User::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    public User getByWechatOpenid(String openid) {
        return lambdaQuery().eq(User::getWechatOpenid, openid).one();
    }
}
