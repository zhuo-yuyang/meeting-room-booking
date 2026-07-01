package com.example.meetingappointment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("booking")
public class Booking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}