package com.dataagg.start.controller;

import com.dataagg.common.entity.SiteSetting;
import com.dataagg.common.result.Result;
import com.dataagg.service.service.SiteSettingService;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.cache.MultiLevelCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SiteSettingController {

    @Autowired
    private SiteSettingService siteSettingService;

    @Autowired
    private MultiLevelCache cache;

    @GetMapping("/api/admin/settings")
    @RequireAuth("ADMIN")
    public Result<Map<String, List<SiteSetting>>> adminList() {
        List<SiteSetting> all = siteSettingService.listAll();
        Map<String, List<SiteSetting>> grouped = all.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getGroupName() != null ? s.getGroupName() : "other",
                        LinkedHashMap::new,
                        Collectors.toList()));
        return Result.success(grouped);
    }

    @PutMapping("/api/admin/settings")
    @RequireAuth("ADMIN")
    public Result<Void> adminUpdate(@RequestBody Map<String, String> body) {
        for (Map.Entry<String, String> entry : body.entrySet()) {
            SiteSetting setting = siteSettingService.getByKey(entry.getKey());
            if (setting != null) {
                setting.setSettingValue(entry.getValue());
                siteSettingService.updateById(setting);
            } else {
                SiteSetting newSetting = new SiteSetting();
                newSetting.setSettingKey(entry.getKey());
                newSetting.setSettingValue(entry.getValue());
                newSetting.setSettingType("text");
                newSetting.setGroupName("channel");
                siteSettingService.save(newSetting);
            }
        }
        cache.evictAll("siteConfig");
        return Result.success();
    }

    @GetMapping("/api/site/config")
    public Result<Map<String, String>> publicConfig() {
        Map<String, String> config = cache.get("siteConfig", "all",
                (Class<Map<String, String>>) (Class<?>) Map.class,
                () -> {
                    List<SiteSetting> all = siteSettingService.listAll();
                    Map<String, String> map = new LinkedHashMap<>();
                    for (SiteSetting s : all) {
                        if (s.getSettingValue() != null) {
                            map.put(s.getSettingKey(), s.getSettingValue());
                        }
                    }
                    return map;
                });
        return Result.success(config);
    }
}
