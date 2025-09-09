package com.chatting.messageclient.service;

import com.chatting.messageclient.dto.restapi.LoginRequest;
import com.chatting.messageclient.dto.restapi.UserRegisterRequest;
import com.chatting.messageclient.json.JsonUtil;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * REST API 요청을 담당하는 클래스
 * - 회원가입, 로그인, 로그아웃, 탈퇴 등을 HTTP 요청으로 서버에 전달
 * - 세션 ID(Session 쿠키)를 관리
 */
public class RestApiService {

    private final TerminalService terminalService;
    private final String url;
    private String sessionId;   // 서버에서 발급받은 세션 ID

    public RestApiService(TerminalService terminalService, String url) {
        this.terminalService = terminalService;
        this.url = "http://" + url; // "localhost:8080" 같은 주소
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * 회원가입
     */
    public boolean register(String username, String password) {
        return request("/api/v1/auth/register", "", new UserRegisterRequest(username, password))
                .filter(httpResponse -> httpResponse.statusCode() == HttpStatus.OK_200.getStatusCode())
                .isPresent();
    }

    /**
     * 회원탈퇴
     */
    public boolean unregister() {
        if (sessionId.isEmpty()) {
            return false;
        }

        return request("/api/v1/auth/unregister", sessionId, null)
                .filter(httpResponse -> httpResponse.statusCode() == HttpStatus.OK_200.getStatusCode())
                .isPresent();
    }

    /**
     * 로그인
     */
    public boolean login(String username, String password) {
        return request("/api/v1/auth/login", "", new LoginRequest(username, password))
                .map(
                        httpResponse -> {
                            if (httpResponse.statusCode() == HttpStatus.OK_200.getStatusCode()) {
                                sessionId = httpResponse.body();
                                return true;
                            }

                            return false;
                        })
                .orElse(false);
    }

    /**
     * 로그아웃
     */
    public boolean logout() {
        if (sessionId.isEmpty()) {
            return false;
        }
        return request("/api/v1/auth/logout", sessionId, null)
                .filter(httpResponse -> httpResponse.statusCode() == HttpStatus.OK_200.getStatusCode())
                .isPresent();
    }

    /**
     * 실제 HTTP 요청을 수행하는 내부 공통 메서드
     */
    private Optional<HttpResponse<String>> request(String path, String sessionId, Object requestObject) {

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                                            .uri(new URI(url + path))
                                            .header("Content-Type", "application/json");

            if (!sessionId.isEmpty()) {
                builder.header("Cookie", "SESSION=" + sessionId);
            }

            if (requestObject != null) {
                JsonUtil.toJson(requestObject)
                        .ifPresent(jsonBody -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)));
            } else {
                builder.POST(HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());

            terminalService.printSystemMessage("Response Status: %d, Body: %s".formatted(httpResponse.statusCode(), httpResponse.body()));

            return Optional.of(httpResponse);
        } catch (Exception ex) {
            terminalService.printSystemMessage("API call failed. cause: %s".formatted(ex.getMessage()));

            return Optional.empty();
        }
    }
}