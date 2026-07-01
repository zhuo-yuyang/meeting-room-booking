package com.example.meetingappointment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.meetingappointment.entity.MeetingRoom;

import java.util.List;

public interface MeetingRoomService extends IService<MeetingRoom> {

    /**
     * 获取所有可用的会议室
     */
    List<MeetingRoom> getAvailableRooms();

    /**
     * 根据容量筛选会议室
     */
    List<MeetingRoom> getRoomsByCapacity(Integer minCapacity);

    boolean removeById(Long id);

    MeetingRoom getById(Long id);
}
