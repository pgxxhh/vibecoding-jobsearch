package com.vibe.jobs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * 时区配置类
 * 
 * 确保系统统一使用UTC时区处理时间，API返回标准ISO 8601格式
 */
@Configuration
public class TimeZoneConfig {
    
    @PostConstruct
    public void setDefaultTimeZone() {
        // 确保JVM默认时区为UTC，保证服务器内部时间处理的一致性
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        // 禁用时间戳格式，使用ISO 8601字符串格式
        // Instant将被序列化为 "2024-01-01T10:00:00Z" 格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}