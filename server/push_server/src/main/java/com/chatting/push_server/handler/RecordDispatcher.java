package com.chatting.push_server.handler;

import com.chatting.push_server.dto.kafka.inbound.RecordInterface;
import com.chatting.push_server.handler.kafka.BaseRecordHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * [레코드 디스패처]
 *
 * 역할:
 * - "들어온 레코드의 실제 타입"에 맞는 핸들러를 찾아 호출한다.
 *   예) AcceptResponseRecord → AcceptResponseRecordHandler
 *
 * 왜 필요한가?
 * - push_server는 다양한 타입의 메시지(초대 응답, 수락 알림, 채널 생성 응답, 일반 메시지 알림 등)를 받는다.
 * - if/else or switch(type)로 거대한 분기문을 만드는 대신,
 *   "타입별 전용 핸들러"를 빈으로 등록해두고, 여기서 자동으로 라우팅하면
 *   - OCP(개방-폐쇄 원칙) 준수: 새 타입 추가 시 디스패처 수정 없이 "핸들러 클래스만 추가"
 *   - 테스트 용이성/가독성/변경 용이성 ↑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordDispatcher {

    /**
     * [핸들러 레지스트리]
     * key: 레코드 클래스(RecordInterface의 구체 타입)
     * val: 해당 타입을 처리하는 핸들러 빈
     *
     * 예) AcceptResponseRecord.class -> AcceptResponseRecordHandler 빈
     */
    private final Map<Class<? extends RecordInterface>, BaseRecordHandler<? extends RecordInterface>> handlerMap = new HashMap<>();

    // 스프링 컨테이너에서 특정 타입의 빈들을 한꺼번에 조회하기 위해 주입
    private final ListableBeanFactory listableBeanFactory;


    /**
     * [디스패치]
     * - record의 런타임 타입을 키로 레지스트리 맵에서 핸들러를 찾아 호출
     */
    public <T extends RecordInterface> void dispatchRecord(T record){
        BaseRecordHandler<T> handler = (BaseRecordHandler<T>) handlerMap.get(record.getClass());

        if(handler != null){
            handler.handleRecord(record);
            return;
        }
        log.error("Handler not found for record type: {}", record.getClass().getSimpleName());

    }


    /**
     * [애플리케이션 시작 시 핸들러 자동 등록]
     * - BaseRecordHandler<T>를 구현한 모든 빈을 스캔해서
     *   "이 핸들러가 처리하는 T 타입"을 리플렉션으로 추출하여 handlerMap에 등록
     * - 새 핸들러를 코드에 추가하고 @Component만 붙이면 자동 등록됨(디스패처 수정 불필요)
     */
    @PostConstruct //그러면 초기화가 된 다음에 이 객체가 만들어지고 나서 여기에 등록되는 메서드가 호출될거다.
    private void prepareRecordHandlerMapping(){
        // 얘를 베이스로 두고 있는 모든 핸들러를 찾아서 담아준다.
        // 스프링 컨테이너에서 BaseRecordHandler 타입(인터페이스)을 구현한 모든 빈을 가져온다.
        // key: beanName, value: bean instance
        Map<String, BaseRecordHandler> beanHandlers = listableBeanFactory.getBeansOfType(BaseRecordHandler.class);

        for(BaseRecordHandler handler : beanHandlers.values()){
            // 각 핸들러에서 "이 핸들러가 처리하는 Request 클래스"를 추출
            Class<? extends RecordInterface> record = extractRecordClass(handler);

            if(record != null){
                handlerMap.put(record, handler);
            }
        }
    }


    /**
     * [핸들러가 처리하는 제네릭 타입 추출]
     * - BaseRecordHandler<SomeRecord> 의 "SomeRecord" 클래스를 꺼내오기 위한 유틸
     * - JDK 동적 프록시가 끼는 경우 등 복잡한 상황에서 타입 정보를 못 읽을 수 있으므로 null 처리
     */
    private Class<? extends RecordInterface> extractRecordClass(BaseRecordHandler handler){
        // handler.getClass() 는 런타임에 해당 객체의 클래스를 반환한다.
        // (예: WriteMessageHandler.class)
        for(Type type: handler.getClass().getGenericInterfaces()){
            if(type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType().equals(BaseRecordHandler.class)){
                // 실제 타입 인자(예: WriteMessage.class)를 반환
                return (Class<? extends RecordInterface>) parameterizedType.getActualTypeArguments()[0];
            }
        }
        // 찾지 못하면 null 반환 (상속구조가 복잡하거나 프록시로 인해 실패할 수 있음)
        return null;
    }

}
