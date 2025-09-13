package com.chatting.backend.constant;

public enum ResultType {

    SUCCESS("Success."),               // 성공
    FAILED("Failed"),                  // 서버 내부 오류 등 일반 실패
    INVALID_ARGS("Invalid arguments."),// 잘못된 입력
    NOT_FOUND("Not found."),           // 존재하지 않는 리소스
    ALREADY_JOINED("Already joined."), // 이미 가입된 경우
    OVER_LIMIT("Over limit."),         // 사용자/인원 수 제한 초과
    NOT_JOINED("Not joined."),         // 가입되지 않은 채널 입장 시도
    NOT_ALLOWED("Unconnected users included.") // 연결관계가 충족되지 않는 요청 등 비허용
    ;

    //NOT_ALLOWED처럼 채널을 생성할 때 연결관계를 확인해야 한다.

    private final String message;

    ResultType(String message) {
        this.message = message;
    }

    //Getter
    /** display 용 메시지(로그/클라이언트) */
    public String getMessage() {
        return message;
    }
}
