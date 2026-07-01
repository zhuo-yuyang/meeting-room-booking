package com.example.meetingappointment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService extends IService<Booking> {

    /**
     * 保存预约（核心：冲突检测 + 自动生成保洁记录）
     */
    Result<String> saveBooking(Long roomId, Long userId, String title,
                               LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 取消预约
     */
    Result<String> cancelBooking(Long id, Long userId);

    // ⭐ 管理员取消预约
    Result<String> adminCancelBooking(Long bookingId);

    /**
     * 获取用户的预约列表
     */
    List<Booking> myBookings(Long userId);

    /**
     * 获取用户的预约列表
     */
    List<Booking> getUserBookings(Long userId);

    /**
     * 检查时间段是否冲突
     */
    boolean checkTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取会议室的所有预约
     */
    List<Booking> getRoomBookings(Long roomId);

    // ⭐ 新增：提前结束会议
    Result<String> endBooking(Long bookingId, Long userId);
    
}
