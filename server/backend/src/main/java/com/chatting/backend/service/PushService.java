package com.chatting.backend.service;

import com.chatting.backend.dto.domain.UserId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;

/** [푸시 알림 전송을 관리하는 서비스]
 * - 푸시 전송이 허용된 메시지 타입을 관리
 * - 실제 푸시 메시지 발송(현재는 log만 찍지만, 실제 구현 시 Firebase/APNs 연동 가능)
 *
 *  특정 메시지가 들어왔을 때 > 푸시 대상인지 확인 > 맞으면 푸시 발송
 *  즉, 푸시 전용 "필터" 역할이다.
 *  "모든 메시지를 푸시로 쏘지 말고, 지정된 메시지만 푸시로 보내라"는 의미.
 */
@Slf4j
@Service
public class PushService {

    private final HashSet<String> pushMessageTypes = new HashSet<>();

    // 푸시로 전송할 수 있는 메시지 타입 등록
    public void registerPushMessageType(String messageType) {
        pushMessageTypes.add(messageType);
    }

    // 푸시 메시지 발송
    public void pushMessage(UserId userId, String messageType, String message){
        //messageType가 포함되어 있으면 푸시가 나갈 수 있다.
        if(pushMessageTypes.contains(messageType)){
            log.info("Push message: {} to user: {}", message, userId);
        }
    }

}
