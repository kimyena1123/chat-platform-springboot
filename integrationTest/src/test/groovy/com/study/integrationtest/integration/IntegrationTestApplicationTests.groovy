package com.study.integrationtest.integration;

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification;

@SpringBootTest
class IntegrationTestApplicationTests extends Specification{

    def "contextLoads"() {
        expect:
        true
    }

}
