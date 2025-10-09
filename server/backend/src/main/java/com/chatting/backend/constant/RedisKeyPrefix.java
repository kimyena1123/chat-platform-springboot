package com.chatting.backend.constant;

/**
 * [RedisKeyPrefix]
 * Redis에 저장되는 데이터의 Key Prefix(접두어)를 한 곳에서 관리하기 위한 상수 클래스입니다.
 *
 * 목적:
 *  - Redis에 여러 종류의 데이터를 저장할 때, Key 이름이 충돌하지 않도록 "message:..." 형태로 네임스페이스를 구분합니다.
 *  - 예: message:user:1, message:channel:42 등으로 구성되어, 한눈에 어떤 데이터인지 파악 가능.
 *  - 코드 전체에서 문자열을 하드코딩하지 않고, 상수를 사용해 유지보수성을 높입니다.
 *
 * 사용 예시:
 *  - redisTemplate.opsForValue().set(RedisKeyPrefix.USER + ":" + userId, userDto);
 *  - redisTemplate.opsForHash().get(RedisKeyPrefix.CHANNEL, channelId);
 */
public class  RedisKeyPrefix {

    public static final String USER_SESSION = "message:user_session";               // 사용자 세션 정보
    public static final String USER = "message:user";                               // 사용자 정보
    public static final String USERNAME = "message:username";                       // username으로 사용자 조회
    public static final String USER_ID = "message:user_id";                         // username → userId 매핑
    public static final String USER_INVITECODE = "message:user_invitecode";         // 사용자 초대코드

    public static final String CONNECTION_STATUS = "message:connection:status";     // 단일 연결 상태
    public static final String CONNECTIONS_STATUS = "message:connections:status";   // 연결된 사용자 목록 상태
    public static final String INVITER_USER_ID = "message:connection:inviter_id";   // 초대한 사용자 ID

    public static final String CHANNEL = "message:channel";                         // 채널 정보
    public static final String CHANNELS = "message:channels";                       // 채널 목록
    public static final String CHANNEL_INVITECODE = "message:channel_invitecode";   // 채널 초대코드
    public static final String JOINED_CHANNEL = "message:joined_channel";           // 가입된 채널 여부
    public static final String PARTICIPANT_IDS = "message:participant_ids";         // 채널 참여자 ID 목록
}
