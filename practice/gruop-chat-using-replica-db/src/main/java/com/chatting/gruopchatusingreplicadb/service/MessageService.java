package com.chatting.gruopchatusingreplicadb.service;

import com.chatting.gruopchatusingreplicadb.dto.Message;
import com.chatting.gruopchatusingreplicadb.repository.MessageRepository;
import com.chatting.gruopchatusingreplicadb.session.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;


/**
 * [MessageService]
 * - 테스트용 메시지 서비스. (실서비스 아님)
 * - 기능
 *    1) 최근 메시지 1건 조회 (/last 용)
 *    2) 특정 message_sequence(ID)로 메시지 조회
 *    3) 메시지 저장/브로드캐스트 (모든 세션에 전파)
 *
 * [Spring Cache를 도입한 목적]
 * - "특정 메시지 조회" 같은 잦은 읽기 요청을 DB까지 가지 않고, 캐시(Redis, Caffeine 등)에서 빠르게 응답하기 위함.
 * - 쓰기/저장 시에는 데이터가 바뀌므로 관련 캐시를 무효화(evict)하여 이후 읽기가 새 데이터를 보게 함.
 *
 * [중요]
 * - 프로젝트에 @EnableCaching 설정이 있어야 아래 @Cacheable / @CacheEvict가 동작합니다.
 * - 캐시 매니저(예: RedisCacheManager, CaffeineCacheManager 등) 설정에 따라 동작/직렬화/TTL이 달라질 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ObjectMapper objectMapper = new ObjectMapper();  // JSON 문자열 <-> 자바 객체 변환기
    private final WebSocketSessionManager webSocketSessionManager;
    private final MessageDataService messageDataService;
    private final MessageRepository messageRepository;


    //사용자가 "/last"를 보내면 가장 최근에 보냈던(상대방이든 나든) 메시지를 보여준다
    /**
     * [최근 메시지 조회 #1]
     * - DB에서 "가장 최근 메시지 1건"을 Optional로 반환.
     * - "/last" 명령을 처리할 때 사용.
     *
     * [캐싱에 대한 설명]
     * - 이 메서드는 파라미터가 없어서 "기본 키 생성 전략"으로는 전부 동일 키가 되어버립니다.
     * - 그래도 캐싱은 "가능"합니다. 단, key를 수동으로 정해줘야 합니다.
     *   (예) @Cacheable(value="message", key="'LAST'", unless="#result == null || #result.isEmpty()")
     * - 현재 예제에서는 의도적으로 캐시를 달지 않았습니다. (아래 @Cacheable 예제를 명확히 보여주려고)
     */
    public Optional<Message> getLastMessage(){

        // 1) 특수 명령 처리: "/last" → 최신 메시지 1건을 조회해 현재 보낸 클라이언트에게만 반환
        // DB에서 가장 최근 메시지(메시지 시퀀스가 가장 큰 것)를 찾는다.
        // 메시지 시퀀스를 출력에 넣는 이유: 테스트 용으로 몇번째 시퀀스인지 보려고.(필수 아님. 그냥 테스트용임)
        return messageRepository
                .findTopByOrderByMessageSequenceDesc()
                // 2) Entity -> DTO 변환
                //    - username과 content를 꺼내서 Message DTO 구성
                //    - content 앞에 "시퀀스:"를 붙여서 테스트 시 눈으로 확인 가능하게 함
                .map(messageEntity -> new Message(messageEntity.getUsername(), messageEntity.getMessageSequence() + ":" + messageEntity.getContent()));
    }

    /**
     * [특정 메시지 조회 #2]
     * - message_sequence(식별자)로 해당 메시지 1건을 Optional로 반환.
     *
     * @Cacheable
     *  - "읽기" 캐시. 메서드를 호출할 때 캐시에 동일 키가 있으면 DB를 아예 건너뛰고 캐시 값을 리턴.
     *  - 캐시에 없으면 메서드를 정상 수행(DB 조회) → 그 결과를 캐시에 저장해 둠.
     *
     * 속성 설명
     *  - value: 사용할 캐시 이름(들). 캐시 영역 이름입니다. (예: "message")
     *           * value와 cacheNames는 같은 의미(알리아스). 보통 value만 씁니다.
     *  - key  : 캐시 키를 SpEL로 지정. "#messageSequenceId" = 파라미터 값 그대로 키로 사용.
     *           * 지정 안 하면 모든 파라미터를 합쳐 기본 KeyGenerator가 만듭니다.
     *  - unless: "이 조건이 true면 캐시에 저장하지 말라"는 의미.
     *            여기서는 결과가 null이면 저장하지 않도록 예시를 달았지만,
     *            반환 타입이 Optional이면 null이 아닌 Optional.empty()가 올 수 있으므로,
     *            실제로는 "#result == null || #result.isEmpty()" 같은 식이 더 정확합니다.
     *
     *  예) 캐시 키
     *    - key="#messageSequenceId"
     *    - messageSequenceId = 42 라면 캐시에는 ("message" 캐시, 키 42)에 결과가 저장됩니다.
     */
    @Cacheable(value = "message", key = "#messageSequenceId", unless = "#result == null")
    public Optional<Message> getMessage(Long messageSequenceId){
        // 1) PK(또는 ID)로 한 건 조회
        return messageRepository.findById(messageSequenceId)
                // 2) Entity -> DTO 변환 (위와 동일하게 "시퀀스:본문" 형태로 표현)
                .map(messageEntity -> new Message(messageEntity.getUsername(), messageEntity.getMessageSequence() + ":" + messageEntity.getContent()));
    }




    /**
     * [핵심 전파 메서드] : 클라이언트가 보낸 payload(JSON)를 파싱 → DB 저장 → 모든 세션에 브로드캐스트
     *
     * @CacheEvict
     *  - "쓰기" 이후 캐시 무효화용. 데이터가 바뀌었으니, 관련 캐시를 비워서 다음 읽기가 새 값을 보게 함.
     *
     * 속성 설명
     *  - value: 지울 캐시 영역 이름. 위 @Cacheable과 동일하게 "message" 캐시 영역을 사용.
     *  - allEntries: true면 해당 캐시 영역의 모든 키를 삭제. (대량 무효화)
     *      * 왜 allEntries=true?
     *        - 이 메서드는 "새 메시지를 저장"합니다. 저장되면 '가장 최근 메시지'가 바뀌고,
     *          특정 ID로 조회하는 캐시 말고도, "최근 메시지" 같은 별도의 캐시 키도 영향을 받을 수 있습니다.
     *        - 테스트/샘플에서는 단순하게 전부 무효화해 일관성 문제를 피했습니다.
     *      * 운영에선 더 정교하게:
     *        - 특정 키만 지우거나(@CacheEvict(key="...")), @CachePut로 캐시를 최신 상태로 갱신하는 방식을 함께 씁니다.
     */
    @Transactional
    @CacheEvict(value = "message", allEntries = true, beforeInvocation = false)
    public void sendMessageToAll(WebSocketSession senderSession, String payload) {
        // 일반 메시지 처리: JSON 파싱 → 저장 → 브로드캐스트
        try {
            // (a) 문자열(JSON) -> DTO 파싱
            //     - JSON 필드명은 Message DTO가 기대하는 필드명과 일치해야 함
            Message receivedMessage = objectMapper.readValue(payload, Message.class);


            // (b) 실험용 플래그: content가 "/exception"이면 DB 저장 로직에서 예외를 발생시키도록 지시
            boolean makeException = receivedMessage.content().equals("/exception");

            // (c) DB에 메시지 저장 (실패 시 예외가 던져짐)
            //     - MessageDataService 안에서 @Transactional, 예외 처리, 쓰기 라우팅 등을 실험할 수 있음
            messageDataService.save(receivedMessage, makeException);

            // (d) 브로드캐스트: 현재 서버에 연결된 모든 세션에게 전송(단, 보낸 사람에게는 재전송하지 않음: 에코 방지)
            webSocketSessionManager
                    .getSessions() // 모든 참가자 세션 목록
                    .forEach(
                            // 보낸 사람(sender)에게는 다시 보내지 않는다.
                            participantSession -> {
                                if (!senderSession.getId().equals(participantSession.getId())) {
                                    // 한 명에게 안전하게 전송하는 유틸 호출
                                    sendMessage(participantSession, receivedMessage);
                                }
                            });
        } catch (Exception ex) {
            // JSON 파싱 실패 등 "프로토콜 위반" 시
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                System.out.println("[DEBUG] 트랜잭션이 활성 상태입니다. 롤백을 진행합니다.");
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                System.out.println("[DEBUG] rollbackOnly 설정 완료");
            }else {
                System.out.println("[DEBUG] 트랜잭션이 활성 상태가 아닙니다. 롤백을 건너뜁니다.");
            }

            String errorMessage = "Invalid protocol.";
            log.error("errorMessage payload: {} from {}", payload, senderSession.getId());

            // 보낸 사람에게만 에러 알림(전체에 뿌리면 안 됨)
            sendMessage(senderSession, new Message("system", errorMessage));
        }
    }


    /**
     * ----------------------------------------------------------------------
     * [개별 전송 유틸] : 특정 WebSocketSession 에게 Message DTO를 안전하게 전송
     *   - 상위 메서드(sendMessageToAll)가 브로드캐스트 흐름을 담당하고,
     *     이 메서드는 "한 명에게 실제로 보내는" 작은 기능에 집중
     *
     *   - 예외가 발생해도 서비스 전체를 깨뜨리지 않도록 catch 후 로그만 남긴다.
     * ----------------------------------------------------------------------
     */
    public void sendMessage(WebSocketSession session, Message message) {
        try {
            // 1) DTO -> JSON 문자열 변환
            String msg = objectMapper.writeValueAsString(message);

            // 2) 실제 전송
            session.sendMessage(new TextMessage(msg));

            log.info("Send message: {} to {}", msg, session.getId());
        } catch (Exception ex) {
            // 개별 사용자 전송 실패는 치명적 오류로 보지 않음(네트워크 일시 장애 등)
            log.error("Failed to send message to {} error: {}", session.getId(), ex.getMessage());
        }
    }

}
