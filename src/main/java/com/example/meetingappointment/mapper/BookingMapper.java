package com.example.meetingappointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.meetingappointment.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    @Update("UPDATE booking SET status = 'ended' WHERE status = 'pending' AND end_time < NOW()")
    int updateExpiredBookings();

    @Select("SELECT COUNT(*) FROM booking WHERE room_id = #{roomId} " +
            "AND status != 'cancelled' " +
            "AND ((start_time <= #{startTime} AND end_time > #{startTime}) " +
            "OR (start_time < #{endTime} AND end_time >= #{endTime}) " +
            "OR (start_time >= #{startTime} AND end_time <= #{endTime}))")
    int checkTimeConflict(@Param("roomId") Long roomId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM booking WHERE user_id = #{userId} AND status = 'pending'")
    int countPendingByUser(@Param("userId") Long userId);

    @Update("UPDATE booking SET status = 'cancelled' WHERE id = #{id} AND status = 'pending'")
    int cancelBooking(@Param("id") Long id);

    // 联表查询预约记录和用户信息
    @Select("SELECT b.*, u.username FROM booking b " +
            "LEFT JOIN user u ON b.user_id = u.id " +
            "WHERE b.room_id = #{roomId} " +
            "ORDER BY b.start_time DESC")
    List<Map<String, Object>> selectBookingsWithUser(@Param("roomId") Long roomId);
}
