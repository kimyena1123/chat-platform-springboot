package com.practice.preview_spring_security_db.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@Configuration
@EnableRedisHttpSession(redisNamespace = "message:user_session", maxInactiveIntervalInSeconds = 300) //기본 30분에서 5분으로 설정
//@EnableRedisIndexedHttpSession //몇가지 기능을 더 가지고 있음
public class RedisSessionConfig {

    //직렬화 될 때 정상 작동하도록
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));

        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
