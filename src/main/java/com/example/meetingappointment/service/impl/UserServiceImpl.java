package com.example.meetingappointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.entity.User;
import com.example.meetingappointment.mapper.UserMapper;
import com.example.meetingappointment.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public boolean register(String username, String password, String email) {
        // 检查用户名是否存在
        if (findByUsername(username) != null) {
            return false;
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole("USER");
        user.setStatus(1);

        return this.save(user);
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            return false;
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }

        // 更新新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        return this.updateById(user);
    }

    // ⭐ 新增：修改密码，返回 Result
    @Override
    public Result<String> updatePassword(Long userId, String oldPassword, String newPassword) {
        // 1. 参数校验
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Result.error(400, "原密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.error(400, "新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error(400, "新密码长度应在6-20位之间");
        }
        if (oldPassword.equals(newPassword)) {
            return Result.error(400, "新密码不能与原密码相同");
        }

        // 2. 查询用户
        User user = this.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 3. 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error(401, "原密码错误");
        }

        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        boolean success = this.updateById(user);

        if (success) {
            log.info("用户 {} 密码修改成功", user.getUsername());
            return Result.success("密码修改成功，请重新登录");
        } else {
            return Result.error(500, "密码修改失败");
        }
    }
}