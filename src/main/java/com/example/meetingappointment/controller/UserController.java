package com.example.meetingappointment.controller;

import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.dto.ChangePasswordDto;
import com.example.meetingappointment.entity.User;
import com.example.meetingappointment.service.UserService;
import com.example.meetingappointment.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userService.getById(userId);
        if (user != null) {
            // 隐藏敏感信息
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error(404, "用户不存在");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result updatePassword(@Valid @RequestBody ChangePasswordDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 尝试修改密码", userId);

        Result Result = userService.updatePassword(
                userId,
                dto.getOldPassword(),
                dto.getNewPassword()
        );

        // 如果密码修改成功，可以选择清除 Token，强制重新登录
        // 前端收到后可以清除 sessionStorage 并跳转到登录页

        return Result;
    }
}
