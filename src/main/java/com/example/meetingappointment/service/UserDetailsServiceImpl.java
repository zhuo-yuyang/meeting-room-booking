package com.example.meetingappointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.meetingappointment.entity.User;
import com.example.meetingappointment.mapper.UserMapper;
import io.micrometer.common.KeyValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 2. 构建权限列表 ⭐ 关键代码
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 从数据库获取角色（假设 role 字段存储的是 "admin" 或 "user"）
        String role = user.getRole();
        if (role != null) {
            // 自动添加 ROLE_ 前缀
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }

        log.info("用户 {} 的角色: {}, 权限: {}", username, role, authorities);
        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())))
                .disabled(user.getStatus() == 0)
                .build();
    }
}
