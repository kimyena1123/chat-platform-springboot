package com.chatting.messageclient;

import com.chatting.messageclient.dto.websocket.outbound.WriteMessage;
import com.chatting.messageclient.handler.CommandHandler;
import com.chatting.messageclient.handler.InboundMessageHandler;
import com.chatting.messageclient.handler.WebSocketMessageHandler;
import com.chatting.messageclient.handler.WebSocketSender;
import com.chatting.messageclient.service.RestApiService;
import com.chatting.messageclient.service.TerminalService;
import com.chatting.messageclient.service.UserService;
import com.chatting.messageclient.service.WebSocketService;

import java.io.IOException;

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

        UserService userService = new UserService();
        InboundMessageHandler inboundMessageHandler = new InboundMessageHandler(userService, terminalService);
        RestApiService restApiService = new RestApiService(terminalService, BASE_URL);
        WebSocketSender webSocketSender = new WebSocketSender(terminalService);
        WebSocketService webSocketService = new WebSocketService(userService, terminalService, webSocketSender, BASE_URL, WEBSOCKET_ENDPOINT);
        webSocketService.setWebSocketMessageHandler(new WebSocketMessageHandler(inboundMessageHandler));
        CommandHandler commandHandler = new CommandHandler(userService, restApiService, webSocketService, terminalService);

        terminalService.printSystemMessage("'/help' Help for commands. ex: /help ");

        while (true) {
            String input = terminalService.readLine("Enter message: ");

            if (!input.isEmpty() && input.charAt(0) == '/') {
                String[] parts = input.split(" ", 2);
                String command = parts[0].substring(1);
                String argument = parts.length > 1 ? parts[1] : "";

                if (!commandHandler.process(command, argument)) {
                    break;
                }

            } else if (!input.isEmpty() && userService.isInChannel()) { //채널에 있을 때만 메시지를 보낼 수 있다.
                terminalService.printMessage("<me>", input);
                webSocketService.sendMessage(new WriteMessage(userService.getChannelId(), input));
            }
        }
    }
}