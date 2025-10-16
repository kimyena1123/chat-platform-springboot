package com.chatting.push_server.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonUtil {
    //전에는 ObjectMapper를 Bean으로 주입받었지만, 이제 주입을 못받는다.
    // 왜? build.gradle 파일을 보면 spring이 없어서 별도로 의존성(jackson-databind)을 넣었기 때문에
    //  spring이 bean으로 안만들어준다.
    //그래서 내가 직접 Bean으로 등록하거나 직접 만들어주는 방법이 있다.

    private final ObjectMapper objectMapper = new ObjectMapper(); //직접 만들어주는 방법 사용

    //JSON 문자열 -> 객체로 변환
    public <T> Optional<T> fromJson(String json, Class<T> clazz) {

        try {
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (Exception ex) {
            log.error("Failed JSON to Object: {}", ex.getMessage());

            return Optional.empty();
        }
    }

    //객체 -> JSON 문자열로 변환
    public Optional<String> toJson(Object object) {
        try {
            return Optional.of(objectMapper.writeValueAsString(object));
        } catch (Exception ex) {
            log.error("Failed Object to JSON: {}", ex.getMessage());

            return Optional.empty();
        }
    }
}
