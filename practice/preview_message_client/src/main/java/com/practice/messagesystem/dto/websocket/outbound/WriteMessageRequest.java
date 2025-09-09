package com.practice.messagesystem.dto.websocket.outbound;

import com.practice.messagesystem.contants.MessageType;

//내가 채팅방에서 메시지를 보낼 때
public class WriteMessageRequest extends BaseRequest {

    //서버가 메시지를 전달하면서 "누가 보냈는지, 무슨 내용인지"를 알아야 함
    private final String username;  //메시지를 보낸 사람의 username
    private final String content;   //메시지의 실제 내용

    public WriteMessageRequest(String username, String content) {
        super(MessageType.WRITE_MESSAGE);
        this.username = username;
        this.content = content;
    }

    //Getter
    public String getUsername() {
        return username;    //보낸 사람 이름
    }

    public String getContent() {
        return content;     //보낸 메시지 내용
    }
}
