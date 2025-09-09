# 이 시스템이 왜 필요한가? 
| 문제                              | 해결                                                           |
| ------------------------------- | ------------------------------------------------------------ |
| WebSocket은 HTTP 요청이 없어서 세션이 만료됨 | `KeepAliveRequest`와 `SessionService.refreshTTL()`로 TTL 연장    |
| REST API에서 로그인 처리 필요            | `RestApiLoginAuthFilter`로 JSON 기반 로그인 구현                     |
| 실시간 채팅 기능 구현                    | WebSocket과 메시지 타입 분류 구조로 확장성 있게 설계                           |
| 세션 기반 인증 + 사용자 정보 조회            | `SecurityContextHolder`를 통해 Spring Security에서 사용자 정보 안전하게 관리 |

---


WebSocket을 통해 실시간 채팅을 지원하면서도, 사용자의 로그인 세션이 유지되도록 관리하는 시스템!

| 구성 요소                | 역할                                                                                         |
| -------------------- | ------------------------------------------------------------------------------------------ |
| `SessionService`     | 세션 TTL 유지 (`keep-alive`)와 현재 로그인한 사용자 이름 조회                                                |
| `MessageUserService` | 사용자 등록, 삭제 등 계정 관련 로직                                                                      |
| WebSocket 관련 DTO     | WebSocket 메시지를 분류하고 처리하기 위한 요청 구조 정의 (`BaseRequest`, `MessageRequest`, `KeepAliveRequest`) |

# SessionService 클래스 - 세션 유지 및 사용자 정보 조회
- 세션을 조회하고, TTL을 연장한다
- 현재 로그인한 사용자 정보를 Spring Security에서 가져온다

### refreshTTL(String httpSessionId) 메소드

- WebSocket이 지속 연결이므로 HTTP 요청이 오지 않는다
- 하지만 Spring Security은 HTTP 요청이 없으면 TTL 만료 후 로그아웃 처리한다 
- 이를 방지하기 위해 클라이언트가 WebSocket으로 KeepAliveRequest를 보내면,

  - 서버는 이 메서드를 호출해서 세션 저장소(ex. Redis)에서 해당 세션을 찾아
  - lastAccessTime을 현재 시간으로 갱신한다 -> TTL 초기화 효과

### getUsername() 메서드

- 현재 세션에서 로그인한 사용자의 username을 가져온다
- Spring Security의 SecurityContextHolder에서 Authentication을 조회하여 가져온다

-----

# MessageUserService 클래스 - 사용자 등록 및 삭제

- 회원가입 및 회원탈퇴 기능
- SessionService를 통해 현재 로그인한 사용자 확인 가능

### addUser(String username String password) 메서드

- 비밀번호는 PasswordEncoder로 암호화
- MessageeUserEntity 객체를 생성해 DB에 저장
- 사용자 등록 완료 후 로그 출력

### removeUser() 메서드

- 로그인한 사용자의 username을 세션에서 조회(SessionService.getUsername())
- usrname으로 DB에서 사용자 조회
- DB에서 삭제 처리
- 로그 출력

# 전체 흐름
1. 사용자가 로그인 > RestApiLoginAuthFilter가 요청 가로채서 인증 처리
   - 인증 성공하면: 세션 생성됨, SecurityContext에 사용자 정보 저장됨, 세션 ID가 클라이언트에게 반환됨
2. 클라이언트가 WebSocket 연결
   - 클라이언트는 WebSocket 연결 시, 로그인 세션 쿠키(JSESSIONID)를 같이 보냄
   - WebSocket 통신을 통해 메시지를 주고받음
3. 사용자가 메시지 전송
   - 서버는 이를 MessageReqeust로 파싱하여 메시지를 ㅓㅊ리
4. 사용자가 KeepAliveRequest 전송
   - WebSocket이지만 세션을 끊기지 않게 하기 위해, 주기적으로 다음 메시지를 전송
```java
{
    'type': "KEEP_ALIVE"
}
```
5. 사용자가 회원탈퇴
   - 클라이언트는 회원탈퇴 요청을 보냄
   - 서버는 SessionService.getUsername()으로 현재 로그인 사용자 확인
   - Db에서 해당 사용자 삭제



