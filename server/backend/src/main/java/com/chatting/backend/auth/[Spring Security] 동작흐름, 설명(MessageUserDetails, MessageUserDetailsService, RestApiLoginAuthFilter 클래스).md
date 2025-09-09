# 1. 전체 구조 개요: 언제, 왜 사용하는가? 
Spring Security에서 로그인을 구현할 때, 로그인 폼에서 사용자가 입력한 username(아이디)를 기반으로 실제 데이터베이스에 저장된 사용자 정보를 찾아서 인증(Authentication)을 수행해야 한다. 

이 과정을 위해 Spring Security는 아래와 같은 흐름으로 동작한다. 

## 인증 흐름(로그인)
1. 사용자가 로그인 폼에 username과 password를 입력한다
2. Spring Security가 UserDetailsService.loadUserByUsername(username) 메서드를 호출한다. 
3. 이 메서드를 오버라이드해서 DB에서 사용자 정보를 조회한다.
4. 조회한 사용자 정보를 UserDetails 인터페이스를 구현한 객체로 래핑해서 반환한다 -> 즉, MessageUserDetails
5. Spring Security가 UserDetails에 담긴 비밀번호와 사용자가 입력한 비밀번호를 비교해 인증을 수행한다. 

# 2. MessageUserDetails 클래스
```java
public class MessageUserDetails implements UserDetails
```

## 왜 UserDeatils를 구현하는가? 
- MessageUserDetails는 인증을 위한 전용 객체이다. 
- Spring Security는 내부적으로 인증된 사용자의 정보를 UserDetails 타입으로 다루기 때문에, 사용할 사용자 정보를 커스터마이징하려면 이 인터페이스를 직접 구현해야 한다. 
- 이 클래스는 DB의 사용자 정보(Entity)를 Spring Security에서 인증 가능한 형식으로 바꿔주는 어댑터 역할을 한다. 

# 3. MessageUserDetailsService 클래스
```java
public class MessageUserDetailsService implements UserDetailsService
```

## 왜 UserDetailsService를 구현하는가?
- Spring Security는 사용자가 로그인할 때 이 서비스의 loadUserByUsername(username)메서드를 호출해 사용자 정보를 가져온다.
- 즉, 이 클래스는 Spring Security와 DB 사이를 연결하는 브릿지 역할을 한다. 

## 왜 여기서(auth) DB 조회를 하는가?
"repository"에서 조회하는건 당연한데, 왜 auth 패키지에서 DB 조회를 하는가? 
- MessageUserDetailsService는 인증 로직에 특화된 서비스이다. 
- 일반적인 도메인 로직과는 별도로, 인증 처리만 담당한다. 
- 따라서 auth 패키지에 위치시키고, 내부적으로는 MessageUserRepository를 통해 DB를 조회한다. 


| 클래스                         | 역할                                 | 사용 시점             |
| --------------------------- | ---------------------------------- | ----------------- |
| `MessageUserDetails`        | Spring Security가 요구하는 인증 사용자 정보 구조 | 인증 도중 사용자 정보 전달   |
| `MessageUserDetailsService` | username 기반 사용자 조회 및 인증 객체 반환      | 로그인 시 DB에서 사용자 검색 |
| `MessageUserEntity`         | 실제 DB 사용자 테이블과 매핑                  | 인증 전후 데이터 저장/조회   |
| `auth` 패키지                  | 인증 관련 로직, 보안 전용 객체, 인증 서비스 관리      | 인증 처리 전체          |

UserDetailsservice는 Spring Security와 DB를 연결하는 브릿지 역할을 한다. 

UserDetails는 Spring security가 사용 가능한 형태로 만들어주는 어댑터 역할을 한다.

그러면 사용자가 로그인폼에서 username, password를 입력해 로그인한다 > UserDetailsService가 DB를 조회해서 username으로 사용자 정보를 가져오고 > UserDetails가 Spring Security가 사용할 수 있는 형태로 만들어준다.


# 4. RestApiLoginAuthFilter 클래스
- 사용자가 로그인을 하면 그 해당 POST 요청을 가로채서 LoginRequest로 JSON을 파싱하고 
- 인증 토큰(UsernamePasswordAuthenticationToken)을 만들어 AuthenticationManager에게 인증을 위임한다.

# 5. AbstractAuthenticationProcessingFilter 클래스
- UsernamePasswordAuthenticationFilter도 이 클래스를 상속한다. 
- RestApiLoginAuthFilter도 이 클래스를 상속한다.
>> 즉, AbstractAuthenticationProcessingFilter는
"로그인 요청을 받고, AuthenticationManager에게 넘기는 구조를 갖춘 인증 필터 템플릿"입니다.

### AbstractAuthenticationProcessingFilter vs UsernamePasswordAuthenticationFilter
```java
AbstractAuthenticationProcessingFilter ← 부모 추상 클래스

├── UsernamePasswordAuthenticationFilter ← 기본 로그인 처리기
├── RestApiLoginAuthFilter ← 우리가 만든 JSON 로그인 처리기

```
→ 따라서 UsernamePasswordAuthenticationFilter는 AbstractAuthenticationProcessingFilter를 상속하는 별도의 클래스이다. 


# 동작 흐름
```java
[Client] ── POST /api/v1/auth/login ──▶ [RestApiLoginAuthFilter]
                                                  │
                                                  ▼
                                   [UsQernamePasswordAuthenticationToken]
                                                  │
                                                  ▼
                                     [AuthenticationManager (ProviderManager)]
                                                  │
                                                  ▼
                                      [DaoAuthenticationProvider]
                                                  │
                                                  ▼
                                   [MessageUserDetailsService → loadUserByUsername()] ──▶ [DB 조회]
                                                  │
                                                  ▼
                                      [MessageUserDetails 생성]
                                                  │
                                                  ▼
                                         (비밀번호 일치 → 인증 성공)
                                                  │
                                                  ▼
                 [successfulAuthentication()] → SecurityContext에 인증 저장
                                                  │
                                                  ▼
                                             세션 ID 응답 → 클라이언트 저장 (로그인 완료)
```

# 각 클래스별 기능 요약
| 클래스                         | 역할        | 설명                                                       |
| --------------------------- | --------- | -------------------------------------------------------- |
| `RestApiLoginAuthFilter`    | 로그인 필터    | 클라이언트에서 보낸 JSON 로그인 요청을 파싱하여 인증 처리                       |
| `SecurityConfig`            | 보안 설정     | CSRF, 기본 로그인 비활성화 / 커스텀 필터 등록 / 로그아웃 핸들러 정의              |
| `AuthenticationManager`     | 인증 처리기    | 인증 토큰을 `DaoAuthenticationProvider`에 전달                   |
| `DaoAuthenticationProvider` | 인증 제공자    | `UserDetailsService`를 통해 사용자 정보 로드 + 패스워드 검증             |
| `MessageUserDetailsService` | 사용자 정보 조회 | DB에서 사용자 정보 조회 (`UserDetailsService` 구현체)                |
| `MessageUserDetails`        | 사용자 정보    | DB 정보를 Spring Security가 사용할 수 있도록 변환 (`UserDetails` 구현체) |


# 전체 순서 흐름 요약
1. 사용자가 로그인 폼에 입력(username, password 입력후 로그인 요청 전송)
2. Spring Security의 인증 필터가 로그인 요청을 가로챔(UseranmeaPasswordAuthenticationFilter가 동작한다)
3. AuthenticaitonManager가 인증 시도
   - 이 토큰을 기반으로 Spring Security 내부의 AuthenticationManager가 인증 시도를 한다
   - 이 과정에서 UserDetailsService.loadUserByUsername(username) 메서드를 호출한다.
4. UserDetailsService에서 사용자 정보 조회(못찾으면 UseranmeNotFoundException -> 로그인 실패 처리)
5. 조회된 사용자 정보를 UserDetails 형태로 반환(MessageUserDetils 객체로 감싸서 Spring Security에 반환한다.)
6. Spring Security가 내부적으로 비밀번호 비교(사용자가 입력한 비밀번호와 DB에서 가져온 userDetails.getpassword() 값을 비교한다. )
7. 일치하면 인증 성공 -> SecurityContext에 저장
   - 인증 성공시, Authenticaiton 객체가 만들어지고, SecurityContextHolder에 저장된다.
   - 이후부터는 @AUthenticationProincipal, SecurityContextHolder.getContext().getAuthemtication()등을 통해 로그인한 사용자 정보를 꺼낼 수 있게 한다.

# 요약
```
[1] 사용자가 JSON 기반 로그인 요청 전송 (POST /api/v1/auth/login)

[2] Spring Security가 이 요청을 필터 체인에서 가로챔
    └ 내가 만든 RestApiLoginAuthFilter가 실행됨

[3] RestApiLoginAuthFilter의 attemptAuthentication() 실행됨
    └ JSON 파싱해서 UsernamePasswordAuthenticationToken 생성
    └ AuthenticationManager에게 authenticate() 위임

[4] AuthenticationManager → DaoAuthenticationProvider 사용
    └ 내부에서 UserDetailsService.loadUserByUsername(username) 호출

[5] MessageUserDetailsService가 DB에서 사용자 조회 → UserDetails 반환
    └ 조회된 결과를 MessageUserDetails(UserDetails 구현체)로 감싸서 반환

[6] DaoAuthenticationProvider가 비밀번호 비교
    └ 일치하면 Authentication 객체 생성 (사용자 인증 완료 상태)

[7] SecurityContextHolder에 인증 정보 저장
    └ HttpSessionSecurityContextRepository를 통해 세션에도 저장됨

[8] 인증 성공 응답 반환 (세션 ID 등 클라이언트에 전달됨)

```

