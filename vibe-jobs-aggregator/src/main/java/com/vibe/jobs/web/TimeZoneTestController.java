package com.vibe.jobs.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;

/**
 * 时区测试控制器 - 用于验证时区配置是否正确
 * 部署后可以删除此文件
 */
@RestController
@RequestMapping("/debug")
public class TimeZoneTestController {
    
    @GetMapping("/timezone")
    public Map<String, Object> getTimezoneInfo() {
        Instant now = Instant.now();
        
        return Map.of(
            "currentUtcTime", now.toString(),              // 当前UTC时间
            "systemTimezone", TimeZone.getDefault().getID(), // JVM默认时区
            "timestamp", now.toEpochMilli(),               // 时间戳
            "message", "API返回标准UTC格式，前端负责转换显示"
        );
    }
}