package com.example.meetingappointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.meetingappointment.entity.MeetingRoom;
import com.example.meetingappointment.mapper.MeetingRoomMapper;
import com.example.meetingappointment.service.MeetingRoomService;
import com.example.meetingappointment.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingRoomServiceImpl extends ServiceImpl<MeetingRoomMapper, MeetingRoom> implements MeetingRoomService {

    @Autowired
    private RedisCache redisCache;

    private static final String ROOM_LIST_KEY = "meeting:room:list";
    private static final String ROOM_KEY_PREFIX = "meeting:room:";
    private static final Long CACHE_EXPIRE_TIME = 3600L; // 1小时

    @Override
    public List<MeetingRoom> getAvailableRooms() {
        System.out.println("========== getAvailableRooms 被调用 ==========");

        List<MeetingRoom> rooms = null;

        // 1. 先从 Redis 缓存获取
        if (redisCache != null) {
            try {
                rooms = redisCache.get(ROOM_LIST_KEY);
                // ⭐ 修复：不要直接调用 rooms.size()，先判断是否为 null
                log.info("从 Redis 缓存获取会议室列表，结果: {}", rooms != null ? "有数据，数量=" + rooms.size() : "无数据");
            } catch (Exception e) {
                log.error("Redis 获取失败: {}", e.getMessage());
            }
        }

        if (rooms != null && !rooms.isEmpty()) {
            log.info("从 Redis 缓存返回会议室列表");
            return rooms;
        }

        // 2. 缓存没有，查询数据库
        log.info("从数据库查询会议室列表");
        try {
            LambdaQueryWrapper<MeetingRoom> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MeetingRoom::getStatus, 1);
            rooms = baseMapper.selectList(wrapper);
            System.out.println("数据库查询结果数量: " + (rooms != null ? rooms.size() : "null"));
        } catch (Exception e) {
            System.err.println("数据库查询失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. 确保 rooms 不为 null
        if (rooms == null) {
            rooms = new ArrayList<>();
            System.out.println("rooms 为 null，已初始化为空列表");
        }

        // 4. 存入 Redis 缓存
        if (!rooms.isEmpty() && redisCache != null) {
            try {
                redisCache.set(ROOM_LIST_KEY, rooms, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
                log.info("会议室列表已存入 Redis 缓存，数量: {}", rooms.size());
            } catch (Exception e) {
                log.error("Redis 存储失败: {}", e.getMessage());
            }
        }

        return rooms;
    }

    @Override
    public List<MeetingRoom> getRoomsByCapacity(Integer minCapacity) {
        LambdaQueryWrapper<MeetingRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingRoom::getStatus, 1)
                .ge(MeetingRoom::getCapacity, minCapacity);
        List<MeetingRoom> rooms = baseMapper.selectList(wrapper);
        return rooms != null ? rooms : new ArrayList<>();
    }

    @Override
    public boolean updateById(MeetingRoom entity) {
        // 更新数据库
        boolean result = super.updateById(entity);
        // 清除缓存
        if (result) {
            clearRoomCache();
            clearSingleRoomCache(entity.getId());
        }
        return result;
    }

    @Override
    public boolean removeById(Long id) {
        boolean result = super.removeById(id);
        if (result) {
            clearRoomCache();
            redisCache.delete(ROOM_KEY_PREFIX + id);
        }
        return result;
    }

    @Override
    public MeetingRoom getById(Long id) {
        // 先从缓存获取单个会议室
        String key = ROOM_KEY_PREFIX + id;
        MeetingRoom room = redisCache.get(key);
        if (room != null) {
            return room;
        }
        // 缓存没有，查数据库
        room = super.getById(id);
        if (room != null) {
            redisCache.set(key, room, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return room;
    }

    private void clearRoomCache() {
        redisCache.delete(ROOM_LIST_KEY);
    }

    private void clearSingleRoomCache(Long id) {
        redisCache.delete(ROOM_KEY_PREFIX + id);
    }
}
