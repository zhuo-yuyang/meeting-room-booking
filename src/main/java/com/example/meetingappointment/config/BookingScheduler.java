package com.example.meetingappointment.config;

import com.example.meetingappointment.mapper.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class BookingScheduler {

    private final BookingMapper bookingMapper;

//    // 每小时执行一次（在每小时的第0分钟执行）
//    @Scheduled(cron = "0 0 * * * *")
//    public void updateExpiredBookings() {
//        int count = bookingMapper.updateExpiredBookings();
//        if (count > 0) {
//            log.info("已更新 {} 条过期预约状态为已结束", count);
//        }
//    }

    // 每5分钟执行一次
    @Scheduled(fixedRate = 300000)
    public void updateExpiredBookings() {
        int count = bookingMapper.updateExpiredBookings();
        if (count > 0) {
            log.info("已更新 {} 条过期预约状态为已结束", count);
        }
    }
}
