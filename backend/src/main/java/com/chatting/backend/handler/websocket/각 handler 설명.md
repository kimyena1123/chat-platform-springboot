# handler 생성 이유

기존에 원래 MessageHandler만 만들어서
- 상대방에게 메시지를 전달하는 역할
- TTL(Keep-Alive) 연장 기능

위 기능들 다 if문과 instanceof를 사용해 담아 동작했었다. 
```java
//[기존 방식]
if (baseRequest instanceof WriteMessageRequest writeMessageRequest) { //메시지 전달하는 코드
        ...
}else if (baseRequest instanceof KeepAliveRequest) { //ttl 연장하는 코드
        ...
}
```

그런데 해당 MessageHandler에 위 기능들을 instanceof로 하게 될 시 기능이 추가될 수록 if문이 끝없이 늘어난다는 단점이 발생.

! 그래서 instanceof 분기처리를 => handler 등록방식으로 교체!

# RequestHandlerDispatcher

- 아래에 있는 핸들러들을 찾아서 관리해줄 Handler

# BaseRequestHandler

- 인터페이스이다. 
- 이 interface가 여기에 대응하는 모든 request들, inbound로 들어오는 걸 제너릭으로 처리할 수 있어야 한다
- 이 메서드를 호출하면 각 핸들러로 자동 분기되는 걸 기대한다.
- 실제 이 핸들러를 사용하는 곳에서는 어떤 객체가 어떤 구현체가 연결돼서 호출되는지는 몰라도 되는 것이다.

# WrtieMessageRequestHandler

- 해당 핸들러는 작성한 메시지를 전달하는 핸들러이다. 
- WriteMessageRequest에는 username과 content가 담겨있다.

# KeepAliveRequestHandler

- 해당 핸들러는 현재 이 채팅 플랫폼을 사용하는 사용자(WriteMessageRequestHandler에서는 메시지를 보내려는 자)의 TTL 연장을 위한 Handler이다. 

----

# 리팩토링
클라이언트가 보내는 메시지 타입별(MessageType) 처리 로직을 if(instanceof ...) 분기문으로 한 곳에 몰아넣지 않고, 각 타입마다 
전용 핸들러(컴포넌트)를 만들고, **런타임에 어떤 핸들러가 그 요청을 처리할 지 자동으로 찾아서 호출**하게 하는 구조다.

if(... instanceof ...) 분기 방식을 > handler 적용 방식으로 할 시, 책임 분리(Separation of concerns)와 학장성(Open/Closed)을 크게 향상 시킨다. 


# 전체 흐름(요약) - 클라이언트 메시지 한 건이 들어와서 처리되는 순서

1. 클라이언트가 WebSocket으로 JSON을 보낸다
```json
//예시
{
  "type": "WRITE_MESSAGE", 
  "username": "user1",
  "content": "hi, my name is user1."
}
```
2. 서버의 MessageHandler.handleTextMessage()가 메시지를 받는다.
3. jsonUtil.fromJson(payload, baseRequest.class)으로 JSON이 실제 서브타입 객체(ex. WriteMessageRequest)로 역직렬화 된다. 
4. MessageHandler는 RequestHandlerDispatcher.dispatchRequest(session, request) 호출한다. 
5. RequestHandlerDispatcher는 request.getClass() (ex. WriteMessageRequest.class)를 키로 HandlerMap에서 적절한 BaseRequestHandler를 찾아낸다. 
6. 찾아낸 핸들러의 handleRequest(session, request)을 호출한다. (ex. WriteMessageRequestHandler.handleReqeust(...)).
7. 각 핸들러는 자신의 책임(ex. DB 저장, TTL 연장, 브로드캐스트 등)을 수행한다. 

즉, MessageHandler는 파이프라인 입구(파싱 + 디스패치) 역할만 하고, 실제 로직은 각 핸들러가 담당한다. 

# 각 클래스/인터페이스 역할

### BaseRequest(추상 DTO)

- JSON에 type 프로퍼티가 있고, Jackson의 @JsonTypeInfo/@JsonSubTypes를 통해 들어온 JSON이 자동으로 알맞은 서브클래스로 역직렬화된다
- ex) WriteMessageRequest, KeepAliveRequest, InviteRequest 등 

### BaseRequestHandler<T extends BaseRequest> 인터페이스

- 모든 실제 핸들러가 구현해야 하는 계약(인터페이스)
- 시그니처: void handleRequest(WebSocketSession webSocketSession, T request);
- T는 처리할 요청 타입(ex. WriteMessageRequest) - 제너릭으로 타입 안정성을 의도함

### WriteMessageRequestHandler / KeepAliveRequestHandler (구체 핸들러들)

- 각각 BaseRequestHandler<WriteMessageRequest>와 BaseRequestHandler<KeepAliveRequeset>를 구현
- 역할
  - WriteMessageRequestHandler: 메시지 DB 저장, 참여자에게 브로드캐스트
  - KeepAliveRequestHandler: 세션 TTL 갱신
- 각각 Spring Bean(@Component)으로 등록되어 DI를 통해 필요한 의존성(Repository, SessionService 등)을 주입받는다. 

### RequestHandlerDispatcher

- 서버 시작 시(Bean 초기화 후, @PostConstruct) 스프링 컨테이너에서 모든 BaseRequestHandler 빈을 찾아 어떤 요청 타입을 처리하는지(핸들러의 제너릭 타입 인자) 추출해서 handlerMap에 requestClass -> handlerBean 형태로 저장한다.
- 런타임에서는 request.getClass()로 handlerMap을 조회해 적잘한 핸들러를 꺼내어 handler.handleRequest(session, request)를 호출한다
- 즉, 핸들러 등록기 + 디스패처 역할을 한다.

# RequestHandlerDispatcher.prepareRequestHandlerMapping() 동작

- listableBeanFactory.getBeansOfType(BaseRequestHandler.class)로 스프링에 등록된 모든 BaseRequestHandler 구현체 빈을 가져온다. 
- 각 핸들러 빈의 클래스의 제너릭 인터페이스(ex. BaseReqeustHandler<WriteMessageRequest>)를 리플렉션으로 검사하여 실제 처리 타입(WriteMessageRequest.class)을 알아낸다(extractRequestClass).
- 그 타입을 키(key)로 해서 handlerMap.put(requestClass, handler) 한다. 

결과: handlerMap이 WriteMessageRequest.class -> WriteMessageRequestHandler bean처럼 초기 설정된다. 


# 런타임 디스패치 동작

- dispatchReqeust(WebSocketSession s, T request)에서 request.getClass() 키로 handlerMap.get(...)한다 
- 핸들러가 있으면 (BaseRequestHandler<T>) handler로 캐스트 후 handleRequest(s, request) 호출
- 핸들러가 없으면 에러 로그(미구현 타입의 메시지 들어왔음)

중요한 점: request.getClass()와 prepareRequestHandlerMapping()에서 사용한 클래스가 정확히 일치해야 한다.
(서브클래스가 또 상속되어 있거나 프록시가 걸려 있으면 키 매칭 실패 가능성 — 그런 경우 ResolvableType 등으로 보완 가능)



----

# Spring Boot / Spring 컨테이너가 시작될 때

1. Spring Boot 애플리케이션이 시작된다(main() 메서드에서 SpringApplication.run() 실행)
2. Spring Boot가 Spring 컨테이너(ApplicationContext) 를 생성하고 부팅을 시작
3. @ComponentScan & Bean 정의 찾기
   - Bean(빈): Spring IoC컨테이너가 관리하는 객체
   - Spring은 빈이 될 후보 클래스를 찾기 위해 컴포넌트 스캔 실행
   - 대표적인 빈 후보 어노테이션: @Component, @Service, @Repository, @Controller/@RestController, @Configuration, @Bean(메서드가 반환하는 객체)
   - 스캔해서 "이 클래스를 컨테이너가 관리해야겠다"라고 등록 -> BeanDefinition 생성
4. BeanDefinition 등록
   - BeanDefinition = "빈에 대한 메타데이터"
   - 어떤 클래승니지
   - 어떤 스코프인지(singleton, prototype 등)
   - 어떤 의존성이 필요한지
   - ApplicationContext 내부의 BeanFactory에 저장
5. Bean 인스턴스 생성
   - BeanFactory가 BeanDefinition을 보고 빈 객체 생성
   - 생성자 주입의 경우 -> 이 시점에 생성자 호출하며 의존성 주입까지 같이 함
6. 의존성 주입(Dependency Injection)
   - 스프링이 각 빈이 필요로 하는 다른 빈을 주입
   - 세가지 방식이 있다.
     - 1. 생성자 주입 -> 생성자를 통해 의존성 주입(추천)
     - 2. 필드 주입 -> @Autowired 필드에 바로 주입(테스트 어려움)
     - 3. 세터 주입 -> 세터 메서드를 통해 주입
   - 왜 주입하나? 객체가 스스로 다른 객체를 만들지 않게 해서, 결합도를 낮추고 테스트와 유지보수를 쉽게 하기 위해서
7. BeanPostProcessor 전처리(postProcessBeforeInitialization)
   - Spring이 제공하는 BeanPostProcessor 인터페이스 구현체들이 빈 초기화 전에 개입
   - 예: @Autowired 처리, #Async 적용, AOP 프로시 생성 등이 이 단계에서 실행
8. 빈 초기화 & BeanPostProcessor 후처리(postProcessAfterInitialization)
   - 초기화 메서드(@PostConstruct or initializingBean.afterPropertiesSet) 실행
   - postProcessAfterInitialization: 
     - 초기화 끝난 빈을 다시 가공할 기회
     - 예: AOP 프록시 래핑, 로깅 설정, 특정 동적 기능 추가
   - 여기서 가공된 빈이 실제로 컨테이너에 등록되어 사용됨
9. 컨테이너 준비 완료 & 요청 처리 시작
   - 모든 싱글톤 빈 생성/주입 완료
   - WebApplicationContext인 경우 -> 내장 톰캣/Jetty 기동
   - HTTP 요청 -> DispatcherServlet -> 컨트롤러 매핑 시작

### 요약
1. Spring Boot 애플리케이션이 시작된다 (SpringApplication.run(...)).
2. 스프링 컨테이너(ApplicationContext)를 만든다.
3. @Component, @Service, @Controller 등 애노테이션을 스캔해서 빈(Bean) 정의를 찾는다.
4. 각 빈을 생성(instantiation)하고, 생성자/필드/세터 의존성 주입(DI, @Autowired 또는 생성자 주입)을 한다. 
5. 빈에 등록된 BeanPostProcessor들이 실행된다(@Autowired, AOP등 내부 처리가 들어감)
6. 빈의 의존성이 모두 주입되면 스프링은 **@PostContruct**가 붙은 메서드를 호출한다(초기화 콜백)
7. 그 다음 postProcessAfterInitialization 단계에서 프록시(AOP) 적용 등 추가 처리가 이루어진다. 
8. 모든 빈이 초기화되면 애플리케이션이 "준비" 상태가 되고 요청을 처리할 수 있게 된다. 

### @PostConstruct란?

- 의미: "이 빈이 생성되고 의존성(DI)이 주입된 다음 한 번 실행할 초기화 코드"를 표시
- 사용에: 내부 캐시 초기화, 다른 빈에서 정보를 수집해서 맵 구성 등
- 주의: 빈이 모두 초기화되기 전(혹은 일부 프록시 적용 전)에 호출될 수 있으므로, 초기화 시점과 프록시 적용 타이밍을 고려해야 한다(보통 문제 없음)


### getClass() 와 제네릭 리플렉션

- handler.getClass()는 런타임에 그 객체의 실제 클래스(ex. WriteMessageRequestHandler.class)를 반환한다. 
- 코드는 handler.getClass().getGenericInterfaces()를 통해 그 클래스가 구현한 인터페이스들의 제네릭 타입 정보를 훑는다. 
- 목적: WriteMessageRequestHandler implements BaseRequestHandler<WriteMessageRequest>처럼 구현되어 있으므로, 런타임에 그 제네릭 인자(WriteMessageRequest)를 알아내서 handlerMap에 WriteMessageRequest.class -> handler로 저장하려는 것
- 이 방식으로 RequestHandlerDispatcher는 "핸들러가 처리하는 요청 타입"을 자동으로 알아낼 수 있다. 

한계/주의
- 자바의 제네릭은 컴파일 타임에 타입 소거(type erasure)가 되지만, 클래스 선언부에 남아있는 제네릭 선언은 리플렉션으로 읽을 수 있다. 그래서 위 방식으로 보통 잘 작동.
- 그러나 스프링이 빈을 프록시(JDK 동적 프록시/CGLIB)로 감쌀 경우, getClass()가 실제 구현 클래스가 아닌 프록시 클래스가 되어 제네릭 정보를 바로 못찾을 수도 있다. 
  - 해결책: 스프링의 ResolvableType 사용하거나, 핸들러가 스스로 처리 타입을 반환하는 메서드/어노테이션을 갖게 하는 방법이 더 견고하다. 


# ReqeustHandlerDispatcher가 실제로 하는 일 (실행 순서)

1. 애플리케이션 시작 > 스프링이 RequestHandlerDispatcher 빈을 만든다(생성자 주입)
2. 의존성(DI) 주입 완료 후 @PostConstruct prepareRequestHandlerMapping() 호출
3. listableBeanFactory.getBeanOfType(BaseRequestHandler.class)로 현재 컨테이너에 등록된 모든 BaseRequestHandler 빈 인스턴스를 가져온다(ex. WriteMessageRequesetHandler, KeepAliveRequestHandler)
4. 각 핸들러 인스턴스에 대해 extreactRequestClass(handler) 실행
   - handler.getClass().getGenericInterfaces()를 보고 BaseRequestHandler<..>인터페이스 선언을 찾아서 그 실제 타입 인자(ex. WriteMessageReqeust.class)를 얻는다
5. 얻은 requestClass를 키로 handlerMap.put(requestClass, handler) 저장.
   - ㄱㄹ과 : handlerMap은 WriteMessageRequestHandler 같으 맵이 된다 
6. 런타임에 MesasgeHandler에서 requestHandlerDispatcher.dispatchRequest(session,parsedRequest)를 호출하면:
   - dispatchRequest는 request.getClass()를 키로 handlerMap.get(...)를 수행하고, 찾으면 그 핸들러의 handleRequest(session, request)를 호출한다.

결국 instanceof 분기 대신 "맵 조회 -> 호출"로 분기 처리를 대체한 것

### RequestHandlerDispatcher에서 Map(멥)을 만든 이유. 사용 이유