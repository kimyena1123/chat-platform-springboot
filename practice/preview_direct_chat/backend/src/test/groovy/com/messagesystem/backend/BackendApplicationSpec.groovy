package com.messagesystem.backend;

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification;

@SpringBootTest(classes = BackendApplication)
class BackendApplicationSpec extends Specification{

    void contextLoads() {
        expect:
        true
    }

}
