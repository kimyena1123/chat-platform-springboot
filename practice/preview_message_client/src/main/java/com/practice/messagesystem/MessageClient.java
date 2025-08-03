package com.practice.messagesystem;

import com.practice.messagesystem.dto.websocket.outbound.MessageRequest;
import com.practice.messagesystem.handler.CommandHandler;
import com.practice.messagesystem.handler.WebSocketMessageHandler;
import com.practice.messagesystem.handler.WebSocketSender;
import com.practice.messagesystem.service.RestApiService;
import com.practice.messagesystem.service.TerminalService;
import com.practice.messagesystem.service.WebSocketService;

import java.io.IOException;

//사용자 입력을 받아 서버와 통신
public class MessageClient {

    public static void main(String[] args) {
        final String BASE_URL = "localhost:8080";
        final String WEBSOCKET_ENDPOINT = "/ws/v1/message";

        TerminalService terminalService;

        try {
            terminalService = TerminalService.create();
        } catch (IOException ex) {
            System.err.println("Failed to run MessageClient");
            return;
        }

        RestApiService restApiService = new RestApiService(terminalService, BASE_URL);
        WebSocketSender webSocketSender = new WebSocketSender(terminalService);
        WebSocketService webSocketService = new WebSocketService(terminalService, webSocketSender, BASE_URL, WEBSOCKET_ENDPOINT);
        webSocketService.setWebSocketMessageHandler(new WebSocketMessageHandler(terminalService));
        CommandHandler commandHandler = new CommandHandler(restApiService, webSocketService, terminalService);

        while (true) {
            String input = terminalService.readLine("Enter message: ");

            if (!input.isEmpty() && input.charAt(0) == '/') {
                String[] parts = input.split(" ", 2);
                String command = parts[0].substring(1);
                String argument = parts.length > 1 ? parts[1] : "";

                if (!commandHandler.process(command, argument)) {
                    break;
                }

            } else if (!input.isEmpty()) {
                terminalService.printMessage("<me>", input);
                webSocketService.sendMessage(new MessageRequest("test client", input));
            }
        }
    }
}
