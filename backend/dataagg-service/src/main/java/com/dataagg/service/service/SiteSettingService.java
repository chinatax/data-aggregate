package com.dataagg.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.SiteSetting;
import com.dataagg.service.mapper.SiteSettingMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SiteSettingService extends ServiceImpl<SiteSettingMapper, SiteSetting> {

    public SiteSetting getByKey(String key) {
        return lambdaQuery().eq(SiteSetting::getSettingKey, key).one();
    }

    public List<SiteSetting> listByGroup(String groupName) {
        return lambdaQuery()
                .eq(SiteSetting::getGroupName, groupName)
                .orderByAsc(SiteSetting::getSortOrder)
                .list();
    }

    public List<SiteSetting> listAll() {
        return lambdaQuery().orderByAsc(SiteSetting::getSortOrder).list();
    }
}
