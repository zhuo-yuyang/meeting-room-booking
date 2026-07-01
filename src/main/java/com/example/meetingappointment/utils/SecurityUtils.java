package com.example.meetingappointment.utils;

import com.example.meetingappointment.entity.User;
import com.example.meetingappointment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static UserService userService;

    // 静态注入（仅用于工具类）
    //    public SecurityUtils(UserService userService) {
    //        SecurityUtils.userService = userService;
    //    }
    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        applicationContext = context;
        userService = applicationContext.getBean(UserService.class);
    }

    private static UserService getUserService() {
        if (userService == null && applicationContext != null) {
            userService = applicationContext.getBean(UserService.class);
        }
        return userService;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前登录用户信息
     */
    public static User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            System.out.println("无法获取当前用户名");
            return null;
        }
        if (userService == null) {
            System.out.println("userService 未注入");
            return null;
        }
        User user = userService.findByUsername(username);
        if (user == null) {
            System.out.println("用户不存在: {"+username+"}");
        }
        return user;
    }
}

