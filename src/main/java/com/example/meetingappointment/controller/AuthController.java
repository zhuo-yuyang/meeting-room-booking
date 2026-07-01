package com.example.meetingappointment.controller;

import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.config.JwtUtils;
import com.example.meetingappointment.dto.LoginDto;
import com.example.meetingappointment.dto.LoginResponseDto;
import com.example.meetingappointment.dto.RegisterDto;
import com.example.meetingappointment.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @PostMapping("/register")
    public Result<LoginResponseDto> register(@Valid @RequestBody RegisterDto dto) {
        boolean success = userService.register(dto.getUsername(), dto.getPassword(), dto.getEmail());
        if (success) {
            return Result.success(new LoginResponseDto(null, dto.getUsername(), "user", "注册成功"));
        }
        return Result.error(409, "用户名已存在");
    }

    @PostMapping("/login")
    public Result<LoginResponseDto> login(@Valid @RequestBody LoginDto dto) {
        System.out.println("========== 登录尝试开始 ==========");
        System.out.println("用户名: " + dto.getUsername());

        try {
            System.out.println("开始认证...");
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword());

            Authentication authentication = authenticationManager.authenticate(authToken);

            System.out.println("认证成功！用户: " + authentication.getName());
            System.out.println("权限: " + authentication.getAuthorities());

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtils.generateToken(userDetails);
            String role = jwtUtils.getRoleFromToken(token);

            return Result.success(new LoginResponseDto(token, dto.getUsername(), role, "登录成功"));

        } catch (Exception e) {
            System.out.println("认证失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error(401, "用户名或密码错误");
        }
    }
}

