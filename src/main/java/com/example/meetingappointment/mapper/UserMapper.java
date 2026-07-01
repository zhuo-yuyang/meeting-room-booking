package com.example.meetingappointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.meetingappointment.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
