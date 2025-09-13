package com.chatting.backend.dto

import com.chatting.backend.dto.websocket.inbound.AcceptRequest
import com.chatting.backend.dto.websocket.inbound.BaseRequest
import com.chatting.backend.dto.websocket.inbound.DisConnectRequest
import com.chatting.backend.dto.websocket.inbound.FetchConnectionsRequest
import com.chatting.backend.dto.websocket.inbound.FetchUserInvitecodeRequest
import com.chatting.backend.dto.websocket.inbound.InviteRequest
import com.chatting.backend.dto.websocket.inbound.KeepAlive
import com.chatting.backend.dto.websocket.inbound.RejectRequest
import com.chatting.backend.dto.websocket.inbound.WriteMessage
import com.chatting.backend.json.JsonUtil
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class RequestTypeMappingSpec extends Specification {

    JsonUtil jsonUtil = new JsonUtil(new ObjectMapper());

    def "DTO 형식의 JSON 문자열을 해당 타입의 DTO로 변환할 수 있다."() {
        given:
        String jsonBody = payload

        when:
        BaseRequest request = jsonUtil.fromJson(jsonBody, BaseRequest).get()

        then:
        request.getClass() == expectedClass
        validate(request)

        where:
        payload                                                                         | expectedClass              | validate
        '{"type": "FETCH_USER_INVITECODE_REQUEST"}'                                     | FetchUserInvitecodeRequest | { req -> (req as FetchUserInvitecodeRequest).getType() == 'FETCH_USER_INVITECODE_REQUEST' }
        '{"type": "FETCH_CONNECTIONS_REQUEST", "status": "ACCEPTED"}'                   | FetchConnectionsRequest    | { req -> (req as FetchConnectionsRequest).status.name() == 'ACCEPTED' }
        '{"type": "INVITE_REQUEST", "userInviteCode": "TestInviteCode123"}'             | InviteRequest              | { req -> (req as InviteRequest).userInviteCode.code() == 'TestInviteCode123' }
        '{"type": "ACCEPT_REQUEST", "username": "testuser"}'                            | AcceptRequest              | { req -> (req as AcceptRequest).username == 'testuser' }
        '{"type": "DISCONNECT_REQUEST", "username": "testuser"}'                        | DisConnectRequest          | { req -> (req as DisConnectRequest).username == 'testuser' }
        '{"type": "REJECT_REQUEST", "username": "testuser"}'                            | RejectRequest              | { req -> (req as RejectRequest).username == 'testuser' }
        '{"type": "WRITE_MESSAGE", "username": "testuser", "content" : "test message"}' | WriteMessage | { req -> (req as WriteMessage).getContent() == 'test message' }
        '{"type": "KEEP_ALIVE"}'                                                        | KeepAlive | { req -> (req as KeepAlive).getType() == 'KEEP_ALIVE' }

    }
}
