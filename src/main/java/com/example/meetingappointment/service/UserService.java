package com.example.meetingappointment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.entity.User;

public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 用户注册
     * @return 是否注册成功
     */
    boolean register(String username, String password, String email);

    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    Result updatePassword(Long userId, String oldPassword, String newPassword);
}
