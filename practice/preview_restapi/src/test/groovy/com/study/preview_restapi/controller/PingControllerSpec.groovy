package com.study.preview_restapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import spock.lang.Specification

// 실제 웹 서버를 띄워서 테스트하기 위해 사용
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PingControllerSpec extends Specification {

    // 테스트용 서버가 실행될 때 사용되는 실제 포트 번호를 주입받음 (예: 54321)
    @LocalServerPort
    int port

    // 테스트 중 API 호출을 수행할 수 있는 스프링 도구
    @Autowired
    TestRestTemplate restTemplate

    def "Ping API는 받은 count 값에 1을 더한 값을 리턴한다"() {
        given:
        def count = 1
        def url = "http://localhost:${port}/api/ping/${count}"

        when:
        // 해당 URL에 대해 GET 요청 수행 (API를 실제로 호출)
        def responseEntity = restTemplate.getForEntity(url, String.class) //end point 호출

        then:
        // 응답 상태 코드가 200 (성공)인지 확인
        responseEntity.statusCode.value() == 200
        // 응답 본문이 기대한 문자열과 일치하는지 확인
        responseEntity.body == "pong : ${count + 1}"
    }
}
