package com.practice.messagesystem.service;

import com.practice.messagesystem.dto.restapi.LoginRequest;
import com.practice.messagesystem.dto.restapi.UserRegisterRequest;
import com.practice.messagesystem.json.JsonUtil;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

//REST API와 관련된 내용 담당
public class RestApiService {

    private final TerminalService terminalService;
    private final String url;
    private String sessionId;

    public RestApiService(TerminalService terminalService, String url) {
        this.terminalService = terminalService;
        this.url = "http://" + url;
    }

    //getter
    public String getSessionId() {
        return sessionId;
    }

    //등록되어있는지의 여부 확인
    public boolean register(String username, String password) {
        return request("/api/v1/auth/register", "", new UserRegisterRequest(username, password))
                .filter(httpResponse -> httpResponse.statusCode() == HttpStatus.OK_200.getStatusCode())
                .isPresent();
    }

    //세션 연결된 다음이라서 username이나 password 매개변수 필요X
    public boolean unregister() {
        if (sessionId.isEmpty()) {
            return false;
        }

        //세션이 있다면 실행 가능함
        return request("/api/v1/auth/unregister", sessionId, null)
                .filter(httpResponse -> httpResponse.statusCode() == HttpStatus.OK_200.getStatusCode())
                .isPresent();
    }


    public boolean login(String username, String password) {
        return request("/api/v1/auth/login", "", new LoginRequest(username, password))
                .map(
                        httpResponse -> {
                            if (httpResponse.statusCode() == HttpStatus.OK_200.getStatusCode()) {
                                //sessionId를 HttpResponse body에서 꺼내서 할당
                                sessionId = httpResponse.body();

                                return true;
                            }

                            //200이 아니면 false
                            return false;
                        })
                .orElse(false);
    }



    //세션 연결된 다음에 동작하는거라서 username, password 필요X
    public boolean logout() {
        if (sessionId.isEmpty()) {
            return false;
        }

        //세션이 있다면 실행 가능함
        return request("/api/v1/auth/logout", sessionId, null)
                .filter(httpResponse -> httpResponse.statusCode() == HttpStatus.OK_200.getStatusCode())
                .isPresent();
    }

    private Optional<HttpResponse<String>> request(
            String path, String sessionId, Object requestObject) {

        try {
            HttpRequest.Builder builder =
                    HttpRequest.newBuilder()
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
