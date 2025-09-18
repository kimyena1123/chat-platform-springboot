package com.chatting.backend.service;

import com.chatting.backend.constant.ResultType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.Channel;
import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.domain.InviteCode;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.projection.ChannelTitleProjection;
import com.chatting.backend.dto.projection.InviteCodeProjection;
import com.chatting.backend.entity.ChannelEntity;
import com.chatting.backend.entity.UserChannelEntity;
import com.chatting.backend.repository.ChannelRepository;
import com.chatting.backend.repository.UserChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 역할: 채널 관련 핵심 비즈니스 로직.
 *
 * 메서드:
 * - create(senderUserId, participantId, title) : Direct 채널 생성
 * - enter(channelId, userId) : 채널 입장 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private static final int LIMIT_HEAD_COUNT = 100;

    private final SessionService sessionService;                // 활성 채널을 Redis에 기록(TTL 관리)
    private final UserConnectionService userConnectionService;
    private final ChannelRepository channelRepository;          // channel 테이블 접근
    private final UserChannelRepository userChannelRepository;  // channel_user 테이블 접근



    /**
     * 사용자가 채널의 정식 참여자인지 여부
     * 사용자가 그 해당 채널에 존재하는지 참여 여부 확인
     */
    public boolean isJoined(ChannelId channelId, UserId userId) {
        return userChannelRepository.existsByUserIdAndChannelId(userId.id(), channelId.id());
    }


    //특정 채널에 속한 모든 사용자들의 ID를 구하는 메서드
    public List<UserId> getParticipantIds(ChannelId channelId) {
        return userChannelRepository.findUserIdsByChannelId(channelId.id())
                .stream()
                .map(userId -> new UserId(userId.getUserId()))
                .toList();
    }

/*
    //사용자가 특정 채널에 온라인 상태인지 확인
    //참여자가 활동중인 채널 = 사용자가 메시지를 보내려는 채널인지 확인
    public boolean isOnline(UserId userId, ChannelId channelId) {
        return sessionService.isOnline(userId, channelId);
    }
*/

    //사용자들이 특정 채널에 온라인 상태인지 확인(현재 해당 채널의 화면을 보고 있는지)해서 온라인인 사용자들의 userId를 리턴
    public List<UserId> getOnlineParticipantIds(ChannelId channelId) {
        //파라미터
        // - 1번쨰 파라미터: 해당 채널 id
        // - 2번째 파라미터: 해당 채널의 참여자
        return sessionService.getOnlineParticipantUserIds(channelId, getParticipantIds(channelId));
    }


    /**
     * channelId로 channel의 invitecode 찾기
     */
    public Optional<InviteCode> getInviteCode(ChannelId channelId) {
        Optional<InviteCode> inviteCode = channelRepository
                .findChannelInviteCodeByChannelId(channelId.id())
                .map(inviteCodeProjection -> new InviteCode(inviteCodeProjection.getInviteCode()));

        if (inviteCode.isEmpty()) {
            log.warn("Invite code is not exist. channelId: {}", channelId);
        }

        return inviteCode;
    }



    /** [1:1, 그룹 채팅방 생성(개설) 메서드]
     *
     * @param senderUserId      채팅방 생성 요청자(개설자)
     * @param participantIds    함께 참여시킬 상대 UserId (1명일수도, 여러명일수도 있다)
     * @param title             채팅방 이름(null/empty 금지)
     * @return Pair(생성된 Channel, ResultType)
     */
    @Transactional //DB에 쓸거니까 transaction으로 묶어준다.
    public Pair<Optional<Channel>, ResultType> create(UserId senderUserId, List<UserId> participantIds, String title) {

        // 1) title 입력 검증(null X, Empty X)
        if (title == null || title.isEmpty()) {
            log.warn("Invalid args : title is empty.");
            return Pair.of(Optional.empty(), ResultType.INVALID_ARGS);
        }

        //headCount 비교
        int headCount = participantIds.size() + 1; //채널 생성자 제외한 참여자 + 채널 생성자

        if(headCount > LIMIT_HEAD_COUNT){ // 채널당 100명 수용 가능. 그것보다 더 많다면 튕겨내기
            log.warn("Over limit of channel. senderUserId(channelCreator): {}, participantIds count = {}, channel title = {}", senderUserId, participantIds.size(), title);
            return Pair.of(Optional.empty(), ResultType.OVER_LIMIT);
        }

        // 1:1일 때: 둘의 관계를 알아보는 getStatus()만 사용해서 ACCEPTED인지만 보면 됐다.
        // 그룹채팅일 때: 채팅방 개설자와 참여자의 관계가 모두 ACCEPTED인지 확인해야 하고, "참여자의 수 = 개설자와 ACCEPTED인 참여자의 수" 여야 한다.
        // 즉, 10명의 참여자가 있다. 그 10명의 참여자와 모두 ACCEPTED여야 한다.
        if(userConnectionService.countCounnectionStatus(senderUserId, participantIds, UserConnectionStatus.ACCEPTED) != participantIds.size()){
            log.warn("Included unconnected user. participantIds: {}", participantIds);
            return Pair.of(Optional.empty(), ResultType.NOT_ALLOWED);
        }

        try {
            /** 두 개의 테이블(channel, channel_user 테이블에 데이터 저장 **/

            // ========== 1) channel 테이블에 insert ==========
            // Entity 생성: 채팅방 이름, 채팅방 참여자 수(나 자신 포함)
            ChannelEntity channelEntity = channelRepository.save(new ChannelEntity(title, headCount));

            // 생성된 channel Entity에서 channelId 구해오기
            Long channelId = channelEntity.getChannelId();

            // ====== 2) channel_user 테이블에 사용자 insert ======
            // 2-1) Entity 생성: 채팅방 참여자(자신 제외)의 userId, 채널ID, 0
            List<UserChannelEntity> userChannelEntities = participantIds.stream().map(
                    participantId -> new UserChannelEntity(participantId.id(), channelId, 0)
            ).collect(Collectors.toList());

            // 2-1) Entity 생성: 채팅방 개설자(나)의 userId, 채널ID, 0
            userChannelEntities.add(new UserChannelEntity(senderUserId.id(), channelId, 0));

            // 테이블에 저장
            userChannelRepository.saveAll(userChannelEntities);

            // ========== 3) 응답용 도메인 DTO 구성 ==========
            Channel channel = new Channel(new ChannelId(channelId), title, headCount);

            return Pair.of(Optional.of(channel), ResultType.SUCCESS);

        } catch (Exception ex) {
            // 트랜잭션은 예외 전파 시 롤백된다. 핸들러에서 FAILED 처리.
            log.error("Create failed. cause: {}", ex.getMessage());
            throw ex;
        }
    }


    /**
     * 채널 입장 처리
     * - 참여자 여부 확인(NOT_JOINED)
     * - 채널 존재 여부 확인(NOT_FOUND)
     * - 세션/Redis에 "현재 활성 채널" 기록(입장 상태 유지, TTL 갱신)
     *
     * @param channelId 입장하려는 채널 식별자
     * @param userId    입장하는 사용자 식별자
     * @return Pair(채널 제목, ResultType)
     */
    public Pair<Optional<String>, ResultType> enter(ChannelId channelId, UserId userId) {

        // 1) 참여자 검증
        if(!isJoined(channelId, userId)) {
            // 사용자가 채널의 참여자가 아니라면
            log.warn("Enter channel failed. User not joined the channel. channelId: {}, userId: {}", channelId, userId);
            return Pair.of(Optional.empty(), ResultType.NOT_JOINED);
        }

        // channelId로 채널명(title) 찾기
        Optional<String> title = channelRepository.findChannelTitleByChannelId(channelId.id()).map(ChannelTitleProjection::getTitle);


        if(title.isEmpty()){ // 찾은 채널명이 비어있는지 확인
            log.warn("Enter channel failed. Channel does not exist. channelId: {}, userId: {}", channelId, userId);
            return Pair.of(Optional.empty(), ResultType.NOT_FOUND);
        }

        // Redis에 "현재 활성 채널" 기록 + TTL 설정(세션 지속성 보조)
        /**
         * [현재 활성 채널 기록]
         * 서버 입장에서 "사용자가 지금 어느 대화방에 들어가 있는지 추적"하기 위해 Redis에 저장
         * 카카오톡을 예시로:
         *  - 내가 친구 A와 대화방을 열고 채팅을 보고 있다.
         *  - 이 상태에서 서버는 "사용자는 지금 A와의 대화방에 있다"라는 정보를 알고 있어야,
         *      - 새로운 메시지가 오면 -> 실시간 알림(Notification)이 아닌 화면에 바로 메시지를 띄워줄 수 있다(채팅방 내부에 있으니까!)
         *      - 반대로 채팅방에 안들어가 있으면 - >푸시 알림("새 메시지가 도착했습니다")으로 알려주도록 분기 가능
         *
         * [TTL 설정]
         * Redis에 기록된 값(예시. user111 -> channel456")에 휴효기간을 두는 것이다.
         * 카카오톡을 예시로:
         * - 내가 채팅방에 들어갔는데, 갑자기 인터넷이 끊기거나, 앱이 강제종료하면?
         * - 서버는 "사용자가 아직도 채탕방 안에 있다"라고 생각하면 잘못된 상태가 남는다
         * - 그래서 TTL을 두어 일정 시간이 지나면 자동으로 기록이 사라지게 한다.
         * - 사용자가 실제로 계속 채팅창에 머물고 있다면, 클라이언트(WebSocket KeepAlive 등)가 주기적으로 호출해서 TTL을 갱신한다.
         *
         * 즉, 현재 활성 채널 기록 = 서버가 사용자가 어느 방에 있는지 추적하기 위해 | TTL 설정 = 비정상 종료 시 자동 정리 + 실제로 접속 유지 중이면 주기적으로 연장
         */
        if(sessionService.setActiveChannel(userId, channelId)){
            return Pair.of(title, ResultType.SUCCESS);
        }

        // 예외 케이스: Redis 기록 실패
        log.error("Enter channel failed. channelId: {}, userId: {}", channelId, userId);
        return Pair.of(Optional.empty(), ResultType.FAILED);

    }
}
