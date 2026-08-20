package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.BlogFileHash;
import com.dataagg.service.mapper.BlogFileHashMapper;
import org.springframework.stereotype.Service;

@Service
public class BlogFileHashService extends ServiceImpl<BlogFileHashMapper, BlogFileHash> {

    public BlogFileHash getByFileName(String fileName) {
        return getOne(new LambdaQueryWrapper<BlogFileHash>()
                .eq(BlogFileHash::getFileName, fileName));
    }

    public void upsert(String fileName, String md5Hash, Long blogId) {
        BlogFileHash existing = getByFileName(fileName);
        if (existing != null) {
            existing.setMd5Hash(md5Hash);
            existing.setBlogId(blogId);
            updateById(existing);
        } else {
            BlogFileHash hash = new BlogFileHash();
            hash.setFileName(fileName);
            hash.setMd5Hash(md5Hash);
            hash.setBlogId(blogId);
            save(hash);
        }
    }

    public void removeByFileName(String fileName) {
        remove(new LambdaQueryWrapper<BlogFileHash>()
                .eq(BlogFileHash::getFileName, fileName));
    }
}
