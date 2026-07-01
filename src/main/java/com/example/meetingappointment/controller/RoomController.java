package com.example.meetingappointment.controller;

import com.example.meetingappointment.common.Result;
import com.example.meetingappointment.dto.RoomDto;
import com.example.meetingappointment.entity.MeetingRoom;
import com.example.meetingappointment.service.MeetingRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final MeetingRoomService meetingRoomService;

    @GetMapping("/list")
    public Result<List<MeetingRoom>> list() {
//        LambdaQueryWrapper<MeetingRoom> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(MeetingRoom::getStatus, 1);
        return Result.success(meetingRoomService.getAvailableRooms());
    }

    @GetMapping("/{id}")
    public Result<MeetingRoom> getById(@PathVariable Long id) {
        return Result.success(meetingRoomService.getById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Result<Boolean> add(@Valid @RequestBody RoomDto dto) {
        MeetingRoom room = new MeetingRoom();
        room.setName(dto.getName());
        room.setCapacity(dto.getCapacity());
        room.setLocation(dto.getLocation());
        room.setEquipment(dto.getEquipment());
        room.setStatus(1);
        return Result.success(meetingRoomService.save(room));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody RoomDto dto) {
        MeetingRoom room = new MeetingRoom();
        room.setId(dto.getId());
        room.setName(dto.getName());
        room.setCapacity(dto.getCapacity());
        room.setLocation(dto.getLocation());
        room.setEquipment(dto.getEquipment());
        room.setStatus(dto.getStatus());
        return Result.success(meetingRoomService.updateById(room));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(meetingRoomService.removeById(id));
    }
}