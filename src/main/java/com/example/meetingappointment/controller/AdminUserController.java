package com.example.meetingappointment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.entity.User;
import com.example.meetingappointment.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // ⭐ 只查询普通用户（role = 'user'），不显示管理员
        wrapper.eq(User::getRole, "user");

        // 关键词搜索（用户名或邮箱）
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getEmail, keyword);
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> result = userService.page(page, wrapper);
        // 隐藏密码
        result.getRecords().forEach(user -> user.setPassword(null));
        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 添加用户
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody User user) {
        // 检查用户名是否已存在
        User existUser = userService.findByUsername(user.getUsername());
        if (existUser != null) {
            return Result.error(409, "用户名已存在");
        }

        // 默认密码 123456
        user.setPassword(passwordEncoder.encode("123456"));
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("user");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        boolean success = userService.save(user);
        return Result.success(success);
    }

    /**
     * 更新用户信息（角色、状态）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody User user) {
        // 不能修改密码（密码修改单独接口）
        User existUser = userService.getById(user.getId());
        if (existUser == null) {
            return Result.error(404, "用户不存在");
        }

        // ⭐ 不能修改为管理员
        if ("admin".equals(user.getRole())) {
            return Result.error(403, "不能将用户设置为管理员");
        }

        // ⭐ 不能修改管理员账号
        if ("admin".equals(existUser.getRole())) {
            return Result.error(403, "不能修改管理员账号");
        }

        // 只允许修改状态、邮箱
        existUser.setStatus(user.getStatus());
        existUser.setEmail(user.getEmail());

        boolean success = userService.updateById(existUser);
        return Result.success(success);
    }

    /**
     * 重置密码（默认 123456）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reset-password/{id}")
    public Result<Boolean> resetPassword(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        user.setPassword(passwordEncoder.encode("123456"));
        boolean success = userService.updateById(user);
        return Result.success(success);
    }

    /**
     * 删除用户
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        // 不能删除管理员自己
        // 可以添加逻辑：检查是否可删除
        boolean success = userService.removeById(id);
        return Result.success(success);
    }
}
