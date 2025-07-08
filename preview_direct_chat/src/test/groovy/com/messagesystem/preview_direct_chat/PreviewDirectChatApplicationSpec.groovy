package com.messagesystem.preview_direct_chat;

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification;

@SpringBootTest(classes = PreviewDirectChatApplication)
class PreviewDirectChatApplicationSpec extends Specification{

    void contextLoads() {
        expect:
        true
    }

}
