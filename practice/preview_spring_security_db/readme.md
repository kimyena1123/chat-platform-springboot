# 🔐 Spring Security 기반 REST API 로그인 & 세션 인증 시스템

이 프로젝트는 **Spring Security**를 기반으로,  
`REST API 형식의 로그인 → 세션을 통한 인증 유지 → 인증 상태 확인 → 로그아웃` 흐름을 **직접 구현**한 예제입니다.  
기본적인 폼 로그인이나 OAuth가 아닌, **REST API 요청 + 세션 방식 인증**을 커스터마이징한 구조입니다.

---

## 💡 인증 흐름 요약

```
[1] REST API 로그인 (/login, POST)
    ↓
[2] 인증 성공 시, SecurityContextHolder에 저장
    ↓
[3] HttpSession 생성 및 JSESSIONID 응답 쿠키 전달
    ↓
[4] 이후 요청 시 JSESSIONID로 세션 인증 확인
    ↓
[5] /check (GET) 요청으로 로그인 여부 확인
    ↓
[6] /logout (POST) 요청으로 세션 만료 → 로그아웃
```

---

## 🧩 주요 구성

| 구성 요소 | 역할 |
|-----------|------|
| `RestApiLoginAuthFilter` | REST 방식 로그인 요청을 처리하고, 세션 인증 저장 |
| `HttpSessionRepository` | 생성된 세션 저장소 관리 (`ConcurrentHashMap` 기반) |
| `CustomHttpSessionHandshakeInterceptor` | WebSocket 핸드셰이크 시 세션 인증 검증 |
| `CheckController` | 현재 로그인 여부를 확인하는 API |
| `SecurityConfig` | 시큐리티 전체 설정 (필터 등록, 세션 관리, 로그아웃 등) |

---

## 1️⃣ REST API 로그인 처리

**📄 파일:** `RestApiLoginAuthFilter.java`

### 🔍 작동 설명
- `/login`으로 들어온 `POST` 요청에서 `username`, `password`를 추출
- `AuthenticationManager`를 통해 로그인 검증
- 인증 성공 시 `SecurityContextHolder`에 인증 정보 저장
- 자동으로 `HttpSession` 생성 → 클라이언트에 `JSESSIONID` 쿠키 전달

### 🔑 주요 코드
```java
UsernamePasswordAuthenticationToken authRequest =
    new UsernamePasswordAuthenticationToken(username, password);

Authentication authentication = authenticationManager.authenticate(authRequest);
SecurityContextHolder.getContext().setAuthentication(authentication);
```

---

## 2️⃣ 세션을 통한 인증 저장

**📄 관련:** `HttpSessionRepository`, Spring Security 내부 처리

### 🔍 작동 설명
- 로그인 성공 시 Spring Security는 자동으로 `HttpSession`을 생성
- 인증 정보 (`Authentication`)를 세션에 저장
- 이후 요청에서 클라이언트가 `JSESSIONID` 쿠키를 함께 보내면,  
  해당 세션을 조회해서 인증 여부를 판단함

### 🔑 확인 로그
```log
세션 생성됨 - sessionId : ABC123XYZ
```

> `HttpSessionRepository`는 세션을 `ConcurrentHashMap`에 저장하고 관리합니다.

---

## 3️⃣ 인증 여부 확인

**📄 파일:** `CheckController.java`

### 🔍 작동 설명
- 클라이언트가 `/check` 엔드포인트로 요청
- Spring Security가 세션에서 `Authentication` 정보를 꺼내서 주입
- 로그인되어 있으면 사용자 이름 반환, 아니면 "인증되지 않음"

### 🔑 주요 코드
```java
@GetMapping("/check")
public String check(Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated()) {
        return "인증된 사용자 : " + authentication.getName();
    }
    return "인증되지 않음";
}
```

---

## 4️⃣ 로그아웃 처리

**📄 설정:** `SecurityConfig.java`

### 🔍 작동 설명
- 클라이언트가 `/logout` 엔드포인트로 요청
- Spring Security가 자동으로 현재 세션을 만료시키고
  인증 정보를 제거함
- `HttpSessionRepository`에서도 세션 제거됨

### 🔑 주요 설정
```java
http.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessHandler((request, response, authentication) -> {
        response.setStatus(HttpServletResponse.SC_OK);
    })
);
```

---

## 🧪 WebSocket 인증 연동 (옵션)

**📄 파일:** `CustomHttpSessionHandshakeInterceptor.java`

### 🔍 작동 설명
- WebSocket 연결 요청 시, `JSESSIONID` 쿠키를 이용해 세션을 검증
- 유효한 세션이 없으면 WebSocket 연결을 차단

### 🔑 주요 코드
```java
for (String cookie : cookies) {
    if (cookie.contains("JSESSIONID")) {
        String sessionId = cookie.split("=")[1];
        HttpSession httpSession = httpSessionRepository.findById(sessionId);
        if (httpSession != null) {
            return true;
        }
    }
}
response.setStatusCode(HttpStatus.UNAUTHORIZED);
return false;
```

---

## 📌 전체 디렉터리 구조 예시

```
src/
└── main/
    └── java/
        └── com/practice/preview_spring_security/
            ├── auth/
            │   ├── RestApiLoginAuthFilter.java
            │   ├── CustomHttpSessionHandshakeInterceptor.java
            ├── config/
            │   └── SecurityConfig.java
            ├── controller/
            │   └── CheckController.java
            ├── session/
            │   └── HttpSessionRepository.java
```

---

## 🚀 요청 흐름 요약 (시퀀스)

```
[Client] --- POST /login (username, password) ---> [RestApiLoginAuthFilter]
        ---> 인증 성공 → SecurityContextHolder 등록
        ---> 세션 생성 → JSESSIONID 쿠키 전달

[Client] --- GET /check (with JSESSIONID cookie) ---> [CheckController]
        ---> 세션에 인증 정보 있으면 → 인증 성공

[Client] --- POST /logout ---> [Spring Security Logout Handler]
        ---> 세션 만료, 인증 제거
```

---

## ✅ 사용 방법

1. 서버 실행 후 Postman 또는 curl로 다음 요청을 테스트해보세요.

### 📌 로그인 (POST)
```http
POST /login
Content-Type: application/x-www-form-urlencoded

username=testuser&password=1234
```
```
curl -i -X POST http://localhost:8080/api/v1/auth/login 
-H "Content-Type: application/json" 
-d '{"username":"testuser", "password":"testpass"}'

```

### 📌 인증 확인 (GET)
```http
GET /check
Cookie: JSESSIONID=<로그인 응답에서 받은 값>
```
```
 curl -X GET http://localhost:8080/api/v1/check 
 -H "Cookie: JSESSIONID=로그인시 부여받은 cookie"
```

### 📌 로그아웃 (POST)
```http
POST /logout
Cookie: JSESSIONID=<로그인 응답에서 받은 값>
```
```
 curl http://localhost:8080/api/v1/auth/logout 
 -H "Cookie: JSESSIONID=로그인시 부여받은 cookie"
 ```

---

## 📎 참고 사항

- 인증 상태는 **세션을 기반으로 유지**되며, JSESSIONID 쿠키를 통해 확인됩니다.
- JWT나 OAuth와는 다르게, 토큰이 아닌 서버 세션 방식입니다.
- 이 구조는 웹 기반이나 WebSocket 연동에서 인증 상태를 공유할 때 유리합니다.

---

## ✍️ 개발자 메모

- Spring Security의 기본 동작을 커스터마이징하여, 필터 레벨에서 인증을 구현
- JWT가 아닌 세션 기반 인증을 이해하고자 할 때 추천되는 설계
- WebSocket 보안 강화(Interceptor)에도 응용 가능
