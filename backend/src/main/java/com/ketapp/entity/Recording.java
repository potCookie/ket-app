package com.ketapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("recording")
public class Recording {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate taskDate;
    private String module;
    private String filePath;
    private String fileUrl;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadedAt;
}
