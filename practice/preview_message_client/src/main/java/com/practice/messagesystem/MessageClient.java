package com.practice.messagesystem;

import com.practice.messagesystem.dto.Message;
import com.practice.messagesystem.handler.WebSocketMessageHandler;
import com.practice.messagesystem.handler.WebSocketSender;
import com.practice.messagesystem.service.TerminalService;
import com.practice.messagesystem.service.WebSocketService;

import java.io.IOException;

//사용자 입력을 받아 서버와 통신
public class MessageClient {

    public static void main(String[] args) {
        final String WEBSOCKET_BASE_URL = "localhost:8080";
        final String WEBSOCKET_ENDPOINT = "/ws/v1/message";

        TerminalService terminalService;
        try {
            terminalService = TerminalService.create();
        } catch (IOException ex) {
            System.err.println("Failed to run MessageClient");

            return;
        }

        WebSocketSender webSocketSender = new WebSocketSender(terminalService);
        WebSocketService webSocketService = new WebSocketService(terminalService, webSocketSender, WEBSOCKET_BASE_URL, WEBSOCKET_ENDPOINT);
        webSocketService.setWebSocketMessageHandler(new WebSocketMessageHandler(terminalService));

        while (true) {
            String input = terminalService.readLine("Enter message: ");

            if (!input.isEmpty() && input.charAt(0) == '/') {
                String command = input.substring(1);

                boolean exit =
                        switch (command) {
                            case "exit" -> {
                                webSocketService.closeSession();
                                yield true;
                            }
                            case "clear" -> {
                                terminalService.clearTerminal();
                                yield false;
                            }
                            case "connect" -> {
                                webSocketService.createSession();
                                yield false;
                            }
                            default -> false;
                        };

                if (exit) {
                    break;
                }
            } else if (!input.isEmpty()) {
                terminalService.printMessage("<me>", input);
                webSocketService.sendMessage(new Message("test client", input));
            }
        }
    }
}
