package com.example.meetingappointment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.entity.Booking;
import com.example.meetingappointment.entity.CleaningRecord;
import com.example.meetingappointment.service.BookingService;
import com.example.meetingappointment.service.CleaningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cleaning")
@RequiredArgsConstructor
public class CleaningController {

    private final CleaningRecordService cleaningRecordService;
    private final BookingService bookingService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {

        // 1. 查询所有保洁记录（不分页，先过滤）
        LambdaQueryWrapper<CleaningRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(CleaningRecord::getStatus, status);
        }
        queryWrapper.orderByAsc(CleaningRecord::getScheduledTime);
        List<CleaningRecord> allRecords = cleaningRecordService.list(queryWrapper);

        // 2. 过滤掉已取消预约对应的保洁记录
        List<Map<String, Object>> filteredRecords = new ArrayList<>();
        for (CleaningRecord record : allRecords) {
            Booking booking = bookingService.getById(record.getBookingId());
            // 只保留预约状态不是 cancelled 的记录
            if (booking != null && !"cancelled".equals(booking.getStatus())) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", record.getId());
                map.put("roomId", record.getRoomId());
                map.put("roomName", record.getRoomName());
                map.put("bookingId", record.getBookingId());
                map.put("scheduledTime", record.getScheduledTime());
                map.put("status", record.getStatus());
                map.put("createTime", record.getCreateTime());
                map.put("finishTime", record.getFinishTime());
                map.put("bookingStatus", booking.getStatus());
                filteredRecords.add(map);
            } else if (booking == null) {
                // 预约记录不存在，也跳过
                log.warn("保洁记录 {} 对应的预约不存在，跳过", record.getId());
            }
        }

        // 3. 手动分页
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredRecords.size());
        List<Map<String, Object>> pageRecords = filteredRecords.subList(
                start > filteredRecords.size() ? 0 : start,
                Math.min(end, filteredRecords.size())
        );

        // 4. 构建分页结果
        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize);
        result.setTotal(filteredRecords.size());
        result.setRecords(pageRecords);

        return Result.success(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/finish/{id}")
    public Result<String> finish(@PathVariable Long id) {
        return cleaningRecordService.finishCleaning(id);
    }
}
