package com.example.meetingappointment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.entity.CleaningRecord;

import java.util.List;

public interface CleaningRecordService extends IService<CleaningRecord> {

    /**
     * 获取保洁记录列表
     * @param status 状态：null-全部，0-待打扫，1-已完成
     */
    List<CleaningRecord> getCleaningList(Integer status);

    /**
     * 标记保洁完成
     */
    Result<String> finishCleaning(Long id);

    /**
     * 获取待打扫数量
     */
    int getPendingCount();
    
    // ⭐ 新增：检查会议室是否有未完成的保洁记录
    boolean hasUnfinishedCleaning(Long roomId);

    /**
     * 检查会议室是否有待打扫的保洁记录
     */
    boolean hasPendingCleaning(Long roomId);
}
