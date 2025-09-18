package com.chatting.backend.dto.projection;

public interface InviteCodeProjection {

    //원래 기존의 사용하던 getConnectionInviteCode()는 나 자신의 초대코드를 알기 위해 사용했던 것.
    //하지만 이제 나 자신의 초대코드 or 채팅방의 초대코드 둘 다 구하는데 사용할 것이라 메서드명을 변경.
    String getInviteCode();
}
