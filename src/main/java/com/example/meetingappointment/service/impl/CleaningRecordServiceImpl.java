package com.example.meetingappointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.entity.CleaningRecord;
import com.example.meetingappointment.mapper.CleaningRecordMapper;
import com.example.meetingappointment.service.CleaningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleaningRecordServiceImpl extends ServiceImpl<CleaningRecordMapper, CleaningRecord>
        implements CleaningRecordService {

    private final CleaningRecordMapper cleaningRecordMapper;

    @Override
    public List<CleaningRecord> getCleaningList(Integer status) {
        LambdaQueryWrapper<CleaningRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CleaningRecord::getStatus, status);
        }
        wrapper.orderByAsc(CleaningRecord::getScheduledTime);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> finishCleaning(Long id) {
        CleaningRecord record = this.getById(id);
        if (record == null) {
            return Result.error("保洁记录不存在");
        }
        if (record.getStatus() == 1) {
            return Result.error("该保洁任务已完成");
        }
        // ⭐ 确保更新时间
        record.setStatus(1);
        record.setFinishTime(LocalDateTime.now());
        boolean success = this.updateById(record);

        int result = cleaningRecordMapper.finishCleaning(id, LocalDateTime.now());
        if (success) {
            log.info("保洁任务完成: id={}, roomName={}", id, record.getRoomName());
            return Result.success("标记完成成功");
        }
        return Result.error("标记完成失败");
    }

    @Override
    public int getPendingCount() {
        LambdaQueryWrapper<CleaningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CleaningRecord::getStatus, 0);
        return (int) this.count(wrapper);
    }

    // ⭐ 实现：检查会议室是否有未完成的保洁记录
    @Override
    public boolean hasUnfinishedCleaning(Long roomId) {
        LambdaQueryWrapper<CleaningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CleaningRecord::getRoomId, roomId)
                .eq(CleaningRecord::getStatus, 0);  // 0 = 待打扫
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean hasPendingCleaning(Long roomId) {
        LambdaQueryWrapper<CleaningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CleaningRecord::getRoomId, roomId)
                .eq(CleaningRecord::getStatus, 0);  // 0 = 待打扫
        return this.count(wrapper) > 0;
    }
}
