package com.example.meetingappointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.meetingappointment.entity.CleaningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface CleaningRecordMapper extends BaseMapper<CleaningRecord> {

    @Update("UPDATE cleaning_record SET status = 1, finish_time = #{finishTime} WHERE id = #{id}")
    int finishCleaning(@Param("id") Long id, @Param("finishTime") LocalDateTime finishTime);
}
