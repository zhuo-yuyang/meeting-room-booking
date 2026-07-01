package com.example.meetingappointment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.meetingappointment.mapper")
public class MeetingAppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingAppointmentApplication.class, args);
    }

}
