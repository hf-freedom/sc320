package com.irrigation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@EnableScheduling
@CrossOrigin
public class SmartIrrigationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartIrrigationApplication.class, args);
        System.out.println("========================================");
        System.out.println("智慧农田灌溉系统启动成功!");
        System.out.println("后端服务地址: http://localhost:8001");
        System.out.println("========================================");
    }
}
