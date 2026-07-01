package com.example.meetingappointment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会议室数据传输对象
 * 用于创建和修改会议室时的参数传递
 */
@Data
public class RoomDto {

    //会议室ID（更新时需要)
    private Long id;

    //会议室名称
    @NotBlank(message = "会议室名称不能为空")
    private String name;

    //容纳人数

    @NotNull(message = "容纳人数不能为空")
    @Min(value = 1, message = "容纳人数至少为1")
    private Integer capacity;

    //位置
    @NotBlank(message = "位置不能为空")
    private String location;

    //设备（投影仪、白板、会议电话等）
    private String equipment;

    //状态：1启用 0禁用
    private Integer status;
}
