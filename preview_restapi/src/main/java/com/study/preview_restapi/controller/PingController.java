package com.study.preview_restapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    @Operation(summary = "1을 더해주는 핑", description = "정수를 받아 1을 증가시켜 리턴한다.") // Swagger에서 API 설명을 표시하기 위해 사용하는 어노테이션
    @GetMapping("ping/{count}")
    public String ping(
            @Parameter(description = "정수형 기준값", example = "123")
            @PathVariable int count
    ) {
        return String.format("pong : %d", count + 1); // 예: count가 1이면 "pong: 2" 반환
    }
}
