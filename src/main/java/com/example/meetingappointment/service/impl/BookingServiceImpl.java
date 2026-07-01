package com.example.meetingappointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.entity.Booking;
import com.example.meetingappointment.entity.CleaningRecord;
import com.example.meetingappointment.entity.MeetingRoom;
import com.example.meetingappointment.mapper.BookingMapper;
import com.example.meetingappointment.mapper.CleaningRecordMapper;
import com.example.meetingappointment.mapper.MeetingRoomMapper;
import com.example.meetingappointment.service.BookingService;
import com.example.meetingappointment.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    private final BookingMapper bookingMapper;
    private final CleaningRecordMapper cleaningRecordMapper;
    private final MeetingRoomMapper meetingRoomMapper;

    @Autowired
    private RedisCache redisCache;
    private static final String BOOKING_CONFLICT_PREFIX = "booking:conflict:";
    private static final String USER_BOOKINGS_PREFIX = "user:bookings:";
    private static final Long CACHE_EXPIRE_TIME = 300L; // 5分钟

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveBooking(Long roomId, Long userId, String title,
                                      LocalDateTime startTime, LocalDateTime endTime) {

        // 1. 时间合法性校验
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) {
            return Result.error("开始时间不能早于当前时间");
        }
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            return Result.error("结束时间必须晚于开始时间");
        }

        // 2. 检查会议室是否存在且可用
        MeetingRoom room = meetingRoomMapper.selectById(roomId);
        if (room == null || room.getStatus() == 0) {
            return Result.error("会议室不存在或已禁用");
        }

        // 3. 冲突检测（核心）
        String conflictKey = BOOKING_CONFLICT_PREFIX + roomId + ":" + startTime + ":" + endTime;
        Boolean hasConflict = redisCache.get(conflictKey);
        if (hasConflict != null && hasConflict) {
            return Result.error("该时段已被预约，请选择其他时间");
        }

        //数据库冲突检测
        int conflict = bookingMapper.checkTimeConflict(roomId, startTime, endTime);
        if (conflict > 0) {
            // 将冲突结果缓存，减少重复查询
            redisCache.set(conflictKey, true, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
            return Result.error("该时段已被预约，请选择其他时间");
        }

        // 4. 用户未开始预约数量限制（最多2个）
        int pendingCount = bookingMapper.countPendingByUser(userId);
        if (pendingCount >= 2) {
            return Result.error("您已有2个未开始的预约，请先取消或等待会议开始");
        }

        // 5. 保存预约记录
        Booking booking = new Booking();
        booking.setRoomId(roomId);
        booking.setUserId(userId);
        booking.setTitle(title);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus("pending");
        this.save(booking);

        // 6. ⭐ 亮点功能：自动生成保洁记录
        CleaningRecord cleaning = new CleaningRecord();
        cleaning.setRoomId(roomId);
        cleaning.setRoomName(room.getName());
        cleaning.setBookingId(booking.getId());
        cleaning.setScheduledTime(endTime);
        cleaning.setStatus(0);  // 0=待打扫
        cleaningRecordMapper.insert(cleaning);
        clearUserBookingsCache(userId);

        log.info("预约成功: roomId={}, userId={}, bookingId={}", roomId, userId, booking.getId());
        log.info("已自动生成保洁记录: cleaningId={}", cleaning.getId());


        return Result.success("预约成功，已通知保洁人员");
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> cancelBooking(Long id, Long userId) {
        // 1. 查询预约记录
        Booking booking = this.getById(id);
        if (booking == null) {
            return Result.error("预约记录不存在");
        }

        // 2. 权限校验：只能取消自己的预约
        if (!booking.getUserId().equals(userId)) {
            return Result.error("只能取消自己的预约");
        }

        // 3. 状态校验：只能取消未开始的预约
        if (!"pending".equals(booking.getStatus())) {
            return Result.error("只能取消未开始的预约");
        }

        // 4. 时间校验：已开始的会议无法取消
        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            return Result.error("已开始的会议无法取消");
        }

        // 5. 更新预约状态
        int result = bookingMapper.cancelBooking(id);
        // 6. ⭐ 取消预约时，自动标记对应的保洁记录为已完成
        if (result > 0) {
            // 查询对应的保洁记录
            LambdaQueryWrapper<CleaningRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CleaningRecord::getBookingId, id);
            CleaningRecord cleaningRecord = cleaningRecordMapper.selectOne(wrapper);

            if (cleaningRecord != null) {
                // 直接删除保洁记录
                cleaningRecordMapper.deleteById(cleaningRecord.getId());
                log.info("取消预约，已删除对应的保洁记录: cleaningId={}, bookingId={}",
                        cleaningRecord.getId(), id);
            }

            log.info("取消预约成功: id={}, userId={}", id, userId);
            return Result.success("取消预约成功");
        }

        return Result.error("取消预约失败");
    }

    @Override
    public List<Booking> myBookings(Long userId) {
        // 先更新过期的预约
        bookingMapper.updateExpiredBookings();
        // 先从 Redis 获取
        String key = USER_BOOKINGS_PREFIX + userId;
        List<Booking> bookings = redisCache.get(key);
        if (bookings != null) {
            log.info("从 Redis 获取用户 {} 的预约列表", userId);
            return bookings;
        }

        // 缓存没有，查数据库
        log.info("从数据库获取用户 {} 的预约列表", userId);
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getUserId, userId)
                .orderByDesc(Booking::getCreateTime);
        bookings = this.list(wrapper);

        // 存入缓存
        if (bookings != null) {
            redisCache.set(key, bookings, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }

        return bookings;
    }

    @Override
    public List<Booking> getUserBookings(Long userId) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getUserId, userId)
                .orderByDesc(Booking::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public boolean checkTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        int conflict = bookingMapper.checkTimeConflict(roomId, startTime, endTime);
        return conflict > 0;
    }

    @Override
    public List<Booking> getRoomBookings(Long roomId) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getRoomId, roomId)
                .orderByDesc(Booking::getStartTime);
        return this.list(wrapper);
    }

    private void clearUserBookingsCache(Long userId) {
        redisCache.delete(USER_BOOKINGS_PREFIX + userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> endBooking(Long bookingId, Long userId) {
        // 1. 查询预约记录
        Booking booking = this.getById(bookingId);
        if (booking == null) {
            return Result.error("预约记录不存在");
        }

        // 2. 权限校验：只能结束自己的会议
        if (!booking.getUserId().equals(userId)) {
            return Result.error("只能结束自己的会议");
        }

        // 3. 状态校验：只有进行中(pending/ongoing)的会议才能提前结束
        if ("ended".equals(booking.getStatus()) || "cancelled".equals(booking.getStatus())) {
            return Result.error("该会议已结束或已取消");
        }

        // 4. 时间校验：会议已经开始才能结束（不能提前结束还没开始的会议）
        if (booking.getStartTime().isAfter(LocalDateTime.now())) {
            return Result.error("会议尚未开始，如需取消请使用取消功能");
        }

        // 5. 更新状态为已结束
        booking.setStatus("ended");
        boolean success = this.updateById(booking);

        if (success) {
            log.info("用户 {} 提前结束会议: bookingId={}", userId, bookingId);
            return Result.success("会议已提前结束");
        } else {
            return Result.error("结束会议失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> adminCancelBooking(Long bookingId) {
        // 1. 查询预约记录
        Booking booking = this.getById(bookingId);
        if (booking == null) {
            return Result.error("预约记录不存在");
        }

        // 2. 状态校验：只能取消待开始的预约
        if (!"pending".equals(booking.getStatus())) {
            return Result.error("只能取消待开始的预约");
        }

        // 3. 时间校验：不能取消已开始的会议
        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            return Result.error("会议已开始，无法取消");
        }

        // 4. 更新状态为已取消
        booking.setStatus("cancelled");
        boolean success = this.updateById(booking);

        // 5. ⭐ 删除对应的保洁记录
        if (success) {
            LambdaQueryWrapper<CleaningRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CleaningRecord::getBookingId, bookingId);
            CleaningRecord cleaningRecord = cleaningRecordMapper.selectOne(wrapper);

            if (cleaningRecord != null) {
                cleaningRecordMapper.deleteById(cleaningRecord.getId());
                log.info("管理员取消预约，已删除保洁记录: cleaningId={}, bookingId={}",
                        cleaningRecord.getId(), bookingId);
            }

            log.info("管理员取消预约成功: bookingId={}", bookingId);
            return Result.success("管理员已取消该预约");
        }

        return Result.error("取消预约失败");
    }

}
