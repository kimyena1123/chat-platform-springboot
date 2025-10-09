package com.chatting.backend.json;

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

    private final ObjectMapper objectMapper;

    //JSON 문자열 -> 객체로 변환
    public <T> Optional<T> fromJson(String json, Class<T> clazz) {

        try {
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (Exception ex) {
            log.error("Failed JSON to Object: {}", ex.getMessage());

            return Optional.empty();
        }
    }

    //List를 처리하는 fromJson
    public <T> List<T> fromJsonToList(String json, Class<T> clazz){
        try {
            return objectMapper.readerForListOf(clazz).readValue(json);
        } catch (Exception ex){
            log.error("Failed JSON to List: {}", ex.getMessage());
            return Collections.emptyList();
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
