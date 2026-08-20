package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.UserFavorite;
import com.dataagg.service.mapper.UserFavoriteMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserFavoriteService extends ServiceImpl<UserFavoriteMapper, UserFavorite> {

    public boolean isFavorited(Long userId, Long bookId) {
        return lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getBookId, bookId)
                .count() > 0;
    }

    public boolean toggle(Long userId, Long bookId) {
        UserFavorite existing = lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getBookId, bookId)
                .one();
        if (existing != null) {
            removeById(existing.getId());
            return false;
        }
        UserFavorite f = new UserFavorite();
        f.setUserId(userId);
        f.setBookId(bookId);
        save(f);
        return true;
    }

    public List<Long> getFavoritedBookIds(Long userId) {
        return lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .list()
                .stream()
                .map(UserFavorite::getBookId)
                .collect(Collectors.toList());
    }
}
