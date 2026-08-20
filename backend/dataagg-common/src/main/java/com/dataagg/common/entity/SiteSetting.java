package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("site_setting")
public class SiteSetting {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("setting_key")
    private String settingKey;

    @TableField("setting_value")
    private String settingValue;

    @TableField("setting_type")
    private String settingType;

    @TableField("group_name")
    private String groupName;

    private String description;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
