package com.example.meetingappointment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cleaning_record")
public class CleaningRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String roomName;
    private Long bookingId;
    private LocalDateTime scheduledTime;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}