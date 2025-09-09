package com.chatting.messageclient.dto.websocket.outbound;

/**
 * 모든 Request의 공통 부모 클래스
 * 어떤 종류의 요청인지 구분하기 위해 MessageType을 가짐
 *
 * 각 request 클래스: 특정 시나리오에서 서버에 필요한 추가 정보를 담아 전송
 *
 * -> 내가 지금 어떤 액션(invite, accept, reject, disconnect, message 등)을 하고 싶다 라고 ㅅ버ㅓ에 전달하는 데이터 구조
 */
public abstract class BaseRequest {

    //여기서 말하는 타입이란. MessageType에 있는 타입들을 말한다.
    private final String type;

    public BaseRequest(String type) {
        this.type = type;
    }

    //Getter
    public String getType() {
        return type;
    }
}