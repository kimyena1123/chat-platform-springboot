package com.practice.messagesystem.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

//Object <-> JSON 문자열 간의 변환을 담당하는 유틸 클래스
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    //JSON 문자열 -> 객체로 변환
    public static <T> Optional<T> fromJson(String json, Class<T> clazz) {

        try {
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (Exception ex) {
            System.err.println("Failed JSON to Object: " + ex.getMessage());

            return Optional.empty();
        }
    }

    //객체 -> JSON 문자열로 변환
    public static Optional<String> toJson(Object object) {
        try {
            return Optional.of(objectMapper.writeValueAsString(object));
        } catch (Exception ex) {
            System.err.println("Failed Object to JSON: " + ex.getMessage());

            return Optional.empty();
        }
    }
}
