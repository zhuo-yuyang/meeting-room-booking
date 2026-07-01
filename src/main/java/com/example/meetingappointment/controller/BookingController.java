package com.example.meetingappointment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.dto.BookingDto;
import com.example.meetingappointment.entity.Booking;
import com.example.meetingappointment.entity.MeetingRoom;
import com.example.meetingappointment.entity.User;
import com.example.meetingappointment.mapper.BookingMapper;
import com.example.meetingappointment.service.BookingService;
import com.example.meetingappointment.service.CleaningRecordService;
import com.example.meetingappointment.service.MeetingRoomService;
import com.example.meetingappointment.service.UserService;
import com.example.meetingappointment.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final MeetingRoomService meetingRoomService;
    private final UserService userService;
    private final BookingMapper bookingMapper;
    private final CleaningRecordService cleaningRecordService;  // 注入

    @PostMapping("/save")
    public Result<String> save(@Valid @RequestBody BookingDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        // 1. 先检查时间冲突
        boolean hasConflict = bookingService.checkTimeConflict(
                dto.getRoomId(),
                dto.getStartTime(),
                dto.getEndTime()
        );
        if (hasConflict) {
            return Result.error("该时段已被预约，请选择其他时间");
        }

        // 2. 再检查该会议室是否有待打扫的保洁记录
        boolean hasPendingCleaning = cleaningRecordService.hasPendingCleaning(dto.getRoomId());
        if (hasPendingCleaning) {
            return Result.error("该会议室尚未打扫完成，暂不能预约");
        }

        // 3. 保存预约
        return bookingService.saveBooking(
                dto.getRoomId(), userId, dto.getTitle(),
                dto.getStartTime(), dto.getEndTime()
        );
    }

    @GetMapping("/my")
    public Result<Page<Map<String, Object>>> myBookings(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long userId = SecurityUtils.getCurrentUserId();

        // 分页查询
        Page<Booking> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getUserId, userId)
                .orderByDesc(Booking::getCreateTime);
        Page<Booking> bookingPage = bookingService.page(page, wrapper);

        // 转换结果，添加会议室名称
        Page<Map<String, Object>> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(bookingPage.getTotal());

        List<Map<String, Object>> records = bookingPage.getRecords().stream()
                .map(booking -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", booking.getId());
                    map.put("roomId", booking.getRoomId());
                    map.put("title", booking.getTitle());
                    map.put("startTime", booking.getStartTime());
                    map.put("endTime", booking.getEndTime());
                    map.put("status", booking.getStatus());
                    map.put("createTime", booking.getCreateTime());

                    // 获取会议室名称
                    MeetingRoom room = meetingRoomService.getById(booking.getRoomId());
                    map.put("roomName", room != null ? room.getName() : "未知会议室");
                    return map;
                })
                .collect(Collectors.toList());

        resultPage.setRecords(records);
        return Result.success(resultPage);
    }

    @DeleteMapping("/cancel/{id}")
    public Result<String> cancel(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return bookingService.cancelBooking(id, userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public Result<Page<Map<String, Object>>> getAllBookings(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String username) {

        // 分页查询
        Page<Booking> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();

        // 如果有筛选条件，需要联表查询，这里简化处理
        // 实际项目中可以用 SQL 联表查询
        wrapper.orderByDesc(Booking::getCreateTime);

        Page<Booking> bookingPage = bookingService.page(page, wrapper);

        // 转换结果，添加会议室名称和用户名
        Page<Map<String, Object>> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setTotal(bookingPage.getTotal());

        List<Map<String, Object>> records = bookingPage.getRecords().stream()
                .map(booking -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", booking.getId());
                    map.put("roomId", booking.getRoomId());
                    map.put("userId", booking.getUserId());
                    map.put("title", booking.getTitle());
                    map.put("startTime", booking.getStartTime());
                    map.put("endTime", booking.getEndTime());
                    map.put("status", booking.getStatus());
                    map.put("createTime", booking.getCreateTime());

                    // 获取会议室名称
                    MeetingRoom room = meetingRoomService.getById(booking.getRoomId());
                    map.put("roomName", room != null ? room.getName() : "未知会议室");

                    // 获取用户名
                    User user = userService.getById(booking.getUserId());
                    map.put("username", user != null ? user.getUsername() : "未知用户");

                    return map;
                })
                .collect(Collectors.toList());

        resultPage.setRecords(records);
        return Result.success(resultPage);
    }
//    /**
//     * 获取某个会议室的预约记录
//     */
//    @GetMapping("/room/{roomId}")
//    public Result<List<Booking>> getRoomBookings(@PathVariable Long roomId) {
//        return Result.success(bookingService.getRoomBookings(roomId));
//    }

    /**
     * 获取会议室的所有预约记录（含用户信息）
     */
    @GetMapping("/room/{roomId}")
//    public Result<List<Map<String, Object>>> @GetMapping("/room/{roomId}")
    public Result<List<Map<String, Object>>> getRoomBookings(@PathVariable Long roomId) {
        List<Map<String, Object>> result = bookingMapper.selectBookingsWithUser(roomId);
        return Result.success(result);
    }

    /**
     * 提前结束会议
     */
    @PutMapping("/end/{id}")
    public Result<String> endBooking(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return bookingService.endBooking(id, userId);
    }

    /**
     * 管理员取消预约（只能取消待开始的预约）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/cancel/{id}")
    public Result<String> adminCancelBooking(@PathVariable Long id) {
        return bookingService.adminCancelBooking(id);
    }
}

