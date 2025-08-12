package com.chatting.backend.handler.websocket;

import com.chatting.backend.dto.websocket.inbound.BaseRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * RequestHandlerDispatcher
 *
 * 역할 요약:
 *  - 애플리케이션 시작(빈 초기화) 시, 스프링 컨테이너에 등록된 모든 BaseRequestHandler 빈을 찾아서
 *    "요청 타입(Class) -> 핸들러 인스턴스" 매핑(handlerMap)을 만든다.
 *  - 런타임에 들어온 BaseRequest 객체에 대해 request.getClass() 로 적절한 핸들러를 찾아 호출(dispatch)한다.
 *
 * 왜 필요한가?
 *  - 여러 요청 타입을 if/instanceof 문으로 분기하지 않고, 각 요청 타입별로 전용 핸들러를 만들어 책임을 분리할 수 있다.
 *  - 새로운 요청 타입 추가 시 MessageHandler를 수정할 필요 없이 새로운 핸들러를 추가하면 자동으로 동작한다.
 *
 * 주의:
 *  - 리플렉션으로 제네릭 타입을 읽어오기 때문에, 프록시(proxy)나 복잡한 상속 구조에서는 타입 추출이 실패할 수 있다.
 *  - 그런 경우에는 Spring의 ResolvableType을 활용하거나, 핸들러에 명시적으로 처리 타입을 리턴하도록 설계하는 편이 더 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked"})
public class RequestHandlerDispatcher {

     /** [이 시스템에 등록된 핸들러를 찾아서 Map에 저장]
     * 요청 타입 클래스(예: WriteMessageRequest.class) 를 키로,
     * 해당 요청을 처리할 BaseRequestHandler 빈 인스턴스를 값으로 저장하는 맵
     *
     * 이유:
     *  - startup 때만 채워지고(prepareRequestHandlerMapping), 그 이후에는 읽기만 하므로 단순 HashMap 사용.
     *  - 멀티스레드 환경에서 읽기 전용으로만 사용하면 ConcurrentHashMap가 아닌 HashMap도 안전하다.
     */
    private final Map<Class<? extends BaseRequest>, BaseRequestHandler<? extends BaseRequest>> handlerMap = new HashMap<>();

    /**
     * Spring 컨테이너에서 특정 타입의 빈들을 조회하기 위한 객체.
     * 이 필드 덕분에 "현재 컨테이너에 등록된 BaseRequestHandler 빈들"을 얻을 수 있다.
     */
    private final ListableBeanFactory listableBeanFactory;

    /**
     * 런타임 디스패치 메서드.
     * 들어온 request 객체의 런타임 클래스로 handlerMap을 조회하여, 해당 handler가 있으면 호출한다.
     *
     * @param webSocketSession 메시지를 보낸 클라이언트의 세션
     * @param request 들어온 요청(이미 BaseRequest의 서브타입으로 역직렬화 되어 있음)
     * @param <T> BaseRequest의 서브타입
     */
    public <T extends BaseRequest> void dispatchRequest(WebSocketSession webSocketSession, T request){
        // request.getClass() 로 런타임 타입(예: WriteMessageRequest.class) 사용
        BaseRequestHandler<T> handler = (BaseRequestHandler<T>) handlerMap.get(request.getClass());

        if(handler != null){
            handler.handleRequest(webSocketSession, request);
            return;
        }
        log.error("Handler not found for request type: {}", request.getClass().getSimpleName());

    }


    /**
     * Bean 초기화 직후에 실행되는 메서드.
     * 스프링이 빈을 만들고 의존성 주입을 끝낸 시점에 호출되므로 여기서 안전하게 다른 핸들러 빈들을 조회하여 매핑을 구성할 수 있다.
     *
     * 각 handler를 component로 등록했기 때문에 Bean으로 등록된다.
     * Spring Boot가 처음 뜨면서 Bean을 초기화하고 Bean 초기화 후에 이 메서드가 호출되면 이 메서드는 현재 등록된 Bean 중에서
     * BaseRequestHandler 타입(handler는 다 BaseRequestHandler를 상속받았다)을 다 찾아서 beanHandlers에 넣어준다.
     * 그런 다음 beanHandlers에 Map에서 찾으면 이 핸들러를 가지고 이 handler를 다시 extractRequestClass 메서드를 호출할 때 넣어준다.
     */
    @PostConstruct //그러면 초기화가 된 다음에 이 객체가 만들어지고 나서 여기에 등록되는 메서드가 호출될거다.
    private void prepareRequestHandlerMapping(){
        // 얘를 베이스로 두고 있는 모든 핸들러를 찾아서 담아준다.
        // 스프링 컨테이너에서 BaseRequestHandler 타입(인터페이스)을 구현한 모든 빈을 가져온다.
        // key: beanName, value: bean instance
        Map<String, BaseRequestHandler> beanHandlers = listableBeanFactory.getBeansOfType(BaseRequestHandler.class);

        for(BaseRequestHandler handler : beanHandlers.values()){
            // 각 핸들러에서 "이 핸들러가 처리하는 Request 클래스"를 추출
            Class<? extends BaseRequest> requestClass = extractRequestClass(handler);

            if(requestClass != null){
                handlerMap.put(requestClass, handler);
            }
        }
    }


    /**
     * 리플렉션으로 전달된 handler 객체가 구현한 인터페이스들을 훑어서,
     * 그중 BaseRequestHandler<SomeRequest> 형태의 ParameterizedType을 찾아 SomeRequest.class 를 반환한다.
     *
     * 동작 원리:
     *  - handler.getClass().getGenericInterfaces() 를 통해 해당 클래스가 구현한 인터페이스들의 타입 정보를 얻는다.
     *  - 만약 어떤 인터페이스가 ParameterizedType 이고(제네릭을 실제로 적어둔 경우),
     *    그 rawType이 BaseRequestHandler.class 이면, 실제 타입 인자(첫 번째)를 꺼내 반환한다.
     *
     * 한계 및 주의:
     *  - 프록시(특히 JDK 동적프록시)로 감싸진 경우 getClass()가 프록시 클래스를 가리키고,
     *    여기에 제네릭 정보가 없을 수 있다. 그런 상황에서는 ResolvableType 같은 Spring 유틸을 사용하자.
     *
     * 배열의 0번쨰 값은 BaseRequest를 상속받은 3개(InviteRequest, WriteMessageRequest, KeepAliveRequest) 중에서만 나온다
     */
    private Class<? extends BaseRequest> extractRequestClass(BaseRequestHandler handler){
        // handler.getClass() 는 런타임에 해당 객체의 클래스를 반환한다.
        // (예: WriteMessageRequestHandler.class)
        for(Type type: handler.getClass().getGenericInterfaces()){
            if(type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType().equals(BaseRequestHandler.class)){
                // 실제 타입 인자(예: WriteMessageRequest.class)를 반환
                return (Class<? extends BaseRequest>) parameterizedType.getActualTypeArguments()[0];
            }
        }
        // 찾지 못하면 null 반환 (상속구조가 복잡하거나 프록시로 인해 실패할 수 있음)
        return null;
    }

}
