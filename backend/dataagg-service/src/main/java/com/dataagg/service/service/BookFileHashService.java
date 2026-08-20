package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.BookFileHash;
import com.dataagg.service.mapper.BookFileHashMapper;
import org.springframework.stereotype.Service;

@Service
public class BookFileHashService extends ServiceImpl<BookFileHashMapper, BookFileHash> {

    public BookFileHash getByFileName(String fileName) {
        return lambdaQuery().eq(BookFileHash::getFileName, fileName).one();
    }

    public void upsertByFileName(String fileName, String md5Hash) {
        BookFileHash existing = getByFileName(fileName);
        if (existing != null) {
            existing.setMd5Hash(md5Hash);
            updateById(existing);
        } else {
            BookFileHash hash = new BookFileHash();
            hash.setFileName(fileName);
            hash.setMd5Hash(md5Hash);
            save(hash);
        }
    }

    public void removeByFileName(String fileName) {
        lambdaUpdate().eq(BookFileHash::getFileName, fileName).remove();
    }
}
