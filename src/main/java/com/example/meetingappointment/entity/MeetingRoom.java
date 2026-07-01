package com.example.meetingappointment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_room")
public class MeetingRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer capacity;
    private String location;
    private String equipment;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

