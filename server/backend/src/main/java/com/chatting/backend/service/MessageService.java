package com.chatting.backend.service;

import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.entity.MessageEntity;
import com.chatting.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/** [메시지 보내기 핵심 비즈니스 로직]
 * - 1) 메시지를 DB에 저장하고
 * - 2) 동일 채널에 참여 중이며, 현재 그 채널을 보고 있는 (online) 사용자들에게 실시간 알림(MessageNotification)을 전달한다.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChannelService channelService;
    private final MessageRepository messageRepository;


    /**
     * [채팅 메시지를 저장하고, 같은 채널에 속해있으면서 실시간 같은 채널을 보고 있는 참여자들에게 실시간 전송(MessageNotification)을 요청한다]
     *
     * 동작순서:
     * 1. DB에 저장
     * 2. 채널 참여자 목록 조회
     * 3. 보낸 사람(나 자신) 제외
     * 4. 현재 그 채널을 보고 있는(Online) 사용자만 선별
     * 5) 선별된 사용자들에게 messageSender.accept(userId) 호출(실시간 전송 요청)
     *
     * @param senderUserId      메시지를 보낸 사용자 ID
     * @param content           전송한 메시지 내용
     * @param channelId         메시자를 전송할 채널 ID
     * @param messageSender     실시간 전송 콜백 (이 사용자에게 메시지를 보내라라고 위임하는 역할)
     */
    public void sendMessage(UserId senderUserId, String content, ChannelId channelId, Consumer<UserId> messageSender) {

        try {
            // 1) 메시지 저장 : 누가, 어떤 내용을 보내는지 저장
            messageRepository.save(new MessageEntity(senderUserId.id(), content));

        } catch (Exception ex) {
            // 저장 실패면 실시간 전송을 하지 않음(유실/불일치 방지).
            log.error("Send message failed. cause: {}", ex.getMessage());
            return;
        }

        // 2) 해당 채널 참여자 ID 전부 조회(해당 채팅 대화방에 들어있는 모든 사람들의 목록)
        List<UserId> participantIds = channelService.getParticipantIds(channelId);

        //나 자신을 제외한 모든 참여자들에게 메시지를 보내야 한다(내가 나한테 보내면 X, 나 제외 같은 톡방에 있는 사람들에게 전송해야함)
        // 3) 보낸 사람인 "나"를 제외하고
        // 4) 현재 이 채널을 보고 있는 (현재 채널에서 활동중인) 참여자에게만
        // 5) 콜백으로 실시간 전송 지시
        participantIds.stream()
                .filter(userId -> !senderUserId.equals(userId))
                .forEach(participantId -> {
                    //"온라인"의 의미: 현재 채널에서 활동 중인(채널에 속해있되 다른 채널에 있는 게 아닌, 채널에 속해있으면서 현재 지금 해당 채널에서 활동중인)
                    if (channelService.isOnline(participantId, channelId)) { //참여자가 활동중인 채널 = 사용자가 메시지를 보내려는 채널인지 확인
                        //해당 (활동중인)참여자에게 메세지를 보내라~라고 위임
                        messageSender.accept(participantId);
                    }
                });
    }
}
