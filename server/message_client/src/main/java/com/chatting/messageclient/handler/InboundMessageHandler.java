package com.chatting.messageclient.handler;

import com.chatting.messageclient.dto.websocket.inbound.*;
import com.chatting.messageclient.json.JsonUtil;
import com.chatting.messageclient.service.TerminalService;
import com.chatting.messageclient.service.UserService;

public class InboundMessageHandler {

    private final UserService userService;
    private final TerminalService terminalService;

    public InboundMessageHandler(UserService userService, TerminalService terminalService) {
        this.userService = userService;
        this.terminalService = terminalService;
    }

    public void handle(String payload) {
        JsonUtil.fromJson(payload, BaseMessage.class)
                .ifPresent(
                        message -> {
                            if (message instanceof MessageNotification messageNotification) {
                                message(messageNotification);
                            } else if (message instanceof FetchUserInvitecodeResponse fetchUserInvitecodeResponse) {
                                fetchUserInviteCode(fetchUserInvitecodeResponse);
                            } else if (message instanceof InviteResponse inviteResponse) {
                                invite(inviteResponse);
                            } else if (message instanceof InviteNotification inviteResponse) {
                                askInvite(inviteResponse);
                            } else if (message instanceof AcceptResponse acceptResponse) {
                                accept(acceptResponse);
                            } else if (message instanceof AcceptNotification acceptNotification) {
                                acceptNotification(acceptNotification);
                            } else if (message instanceof RejectResponse rejectResponse) {
                                reject(rejectResponse);
                            } else if (message instanceof DisconnectResponse disconnectResponse) {
                                disconnect(disconnectResponse);
                            } else if (message instanceof FetchConnectionsResponse fetchConnectionsResponse) {
                                fetchConnections(fetchConnectionsResponse);
                            } else if (message instanceof FetchChannelsListResponse fetchChannelsListResponse) {
                                fetchChannels(fetchChannelsListResponse);
                            } else if (message instanceof FetchChannelInviteCodeResponse fetchChannelInviteCodeResponse) {
                                fetchChannelInviteCode(fetchChannelInviteCodeResponse);
                            } else if (message instanceof CreateResponse createResponse) {
                                create(createResponse);
                            } else if (message instanceof JoinNotification joinNotification) {
                                joinNotification(joinNotification);
                            } else if (message instanceof JoinResponse joinResponse) {
                                join(joinResponse);
                            } else if (message instanceof EnterResponse enterResponse) {
                                enter(enterResponse);
                            } else if (message instanceof LeaveResponse leaveResponse) {
                                leave(leaveResponse);
                            } else if (message instanceof QuitResponse quitResponse) {
                                quit(quitResponse);
                            } else if (message instanceof ErrorResponse errorResponse) {
                                error(errorResponse);
                            }
                        });
    }

    private void message(MessageNotification messageNotification) {
        terminalService.printMessage(messageNotification.getUsername(), messageNotification.getContent());
    }

    private void fetchUserInviteCode(FetchUserInvitecodeResponse fetchUserInvitecodeResponse) {
        terminalService.printSystemMessage("내 초대코드: %s".formatted(fetchUserInvitecodeResponse.getInviteCode()));
    }

    private void invite(InviteResponse inviteResponse) {
        terminalService.printSystemMessage("Invite %s result: %s".formatted(inviteResponse.getInviteCode(), inviteResponse.getStatus()));
    }

    private void askInvite(InviteNotification inviteNotification) {
        terminalService.printSystemMessage("%s의 연결초대(친구초대)를 수락하겠습니까??".formatted(inviteNotification.getUsername()));
    }

    private void accept(AcceptResponse acceptResponse) {
        terminalService.printSystemMessage("%s와(과) 연결되었습니다".formatted(acceptResponse.getUsername()));
    }

    private void acceptNotification(AcceptNotification acceptNotification) {
        terminalService.printSystemMessage("%s와(과) 연결되었습니다.".formatted(acceptNotification.getUsername()));
    }

    private void reject(RejectResponse rejectResponse) {
        terminalService.printSystemMessage("%s의 연결을 거절했습니다. 상태: %s".formatted(rejectResponse.getUsername(), rejectResponse.getStatus()));
    }

    private void disconnect(DisconnectResponse disconnectResponse) {
        terminalService.printSystemMessage("%s와(과) 연결이 끊겼습니다. 상태: %s".formatted(disconnectResponse.getUsername(), disconnectResponse.getStatus()));
    }

    private void fetchConnections(FetchConnectionsResponse fetchConnectionsResponse) {
        fetchConnectionsResponse
                .getConnections()
                .forEach(
                        connection ->
                                terminalService.printSystemMessage(
                                        "%s : %s".formatted(connection.username(), connection.status())));
    }

    private void fetchChannels(FetchChannelsListResponse fetchChannelsListResponse) {
        fetchChannelsListResponse.getChannels().forEach(channel -> terminalService.printSystemMessage("%s: %s (%d)"
                .formatted(channel.channelId(), channel.title(), channel.headCount())));
    }

    private void fetchChannelInviteCode(FetchChannelInviteCodeResponse fetchChannelInviteCodeResponse) {
        terminalService.printSystemMessage("<%s> 채널의 초대코드 요청. 채널의 초대코드: %s".formatted(fetchChannelInviteCodeResponse.getChannelId(), fetchChannelInviteCodeResponse.getInviteCode()));
    }

    private void create(CreateResponse createResponse) {
        //채널 생성에 성공했다면
        terminalService.printSystemMessage("채널 생성 성공. <채널 아이디: %s, 채널명: %s>".formatted(createResponse.getChannelId(), createResponse.getTitle()));
    }

    private void joinNotification(JoinNotification joinNotification) {
        //채널 생성에 성공했다면
        terminalService.printSystemMessage("채널에 가입되었습니다. <요청자 아이디: %s, 채널명: %s>".formatted(joinNotification.getChannelId(), joinNotification.getTitle()));
    }

    private void join(JoinResponse joinResponse) {
        terminalService.printSystemMessage("채널 요청코드로 요청한 채널에 가입이 되었습니다. <%s: %s>".formatted(joinResponse.getChannelId(), joinResponse.getTitle()));
    }

    private void enter(EnterResponse enterResponse) {
        userService.moveToChannel(enterResponse.getChannelId());
        terminalService.printSystemMessage("채팅방에 입장했습니다. <채널 아이디: %s, 채널명: %s>".formatted(enterResponse.getChannelId(), enterResponse.getTitle()));
    }

    private void leave(LeaveResponse leaveResponse) {
        terminalService.printSystemMessage("채팅방을 나갔습니다. <채널아이디: %s>".formatted(userService.getChannelId()));
        userService.moveToLobby();
    }

    //로비에서만 사용할 수 있다. 로비로 설정할 필요 X
    private void quit(QuitResponse quitResponse) {
        terminalService.printSystemMessage("채팅방을 탈퇴했습니다. <채널아이디: %s>".formatted(quitResponse.getChannelId()));
    }

    private void error(ErrorResponse errorResponse) {
        terminalService.printSystemMessage("Error %s: %s".formatted(errorResponse.getMessageType(), errorResponse.getMessage()));
    }
}