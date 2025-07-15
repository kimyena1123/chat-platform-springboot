package com.practice.messagesystem.handler;

import com.practice.messagesystem.dto.Message;
import com.practice.messagesystem.json.JsonUtil;
import com.practice.messagesystem.service.TerminalService;
import jakarta.websocket.MessageHandler;

//서버에서 받은 텍스트 메시지를 처리하는 클래스
public class WebSocketMessageHandler implements MessageHandler.Whole<String> {

    private final TerminalService terminalService;

    public WebSocketMessageHandler(TerminalService terminalService) {
        this.terminalService = terminalService;
    }

    @Override
    public void onMessage(String payload) {
        //JSON 문자열 -> Message 객체로 변환
        JsonUtil.fromJson(payload, Message.class)
                .ifPresent(message -> terminalService.printMessage(message.username(), message.content()));
    }
}
