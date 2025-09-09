package com.chatting.messageclient.service;

import java.io.IOException;
import java.nio.file.Paths;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

/**
 * 콘솔에서 입력과 출력을 담당하는 클래스
 * - 사용자가 입력한 문자열을 읽어옴
 * - 서버/시스템에서 전달된 메시지를 터미널 위에 출력
 * - 터미널 화면을 지우는 기능 제공
 */
public class TerminalService {

    private Terminal terminal;      // 실제 콘솔 화면 조작
    private LineReader lineReader;  // 입력과 출력 도구

    private TerminalService() {
    }

    /**
     * 정적 팩토리 메서드로 객체 생성
     */
    public static TerminalService create() throws IOException {
        TerminalService terminalService = new TerminalService();

        try {
            // 터미널 초기화
            terminalService.terminal = TerminalBuilder.builder().system(true).build();
        } catch (IOException ex) {
            System.err.println("Failed to create TerminalService. error: " + ex.getMessage());
            throw ex;
        }

        // 입력 히스토리 파일 경로 설정
        terminalService.lineReader = LineReaderBuilder.builder()
                                        .terminal(terminalService.terminal)
                                        .variable(LineReader.HISTORY_FILE, Paths.get("./data/history.txt"))
                                        .build();

        return terminalService;
    }

    /**
     * 프롬프트를 띄우고 입력을 받는다.
     * 입력 후 커서를 위로 올리고 기존 입력을 지워서 화면을 깔끔하게 유지.
     */
    public String readLine(String prompt) {
        String input = lineReader.readLine(prompt);
        terminal.puts(InfoCmp.Capability.cursor_up);
        terminal.puts(InfoCmp.Capability.delete_line);
        terminal.flush();
        return input;
    }

    /**
     * 채팅 메시지를 콘솔에 출력
     */
    public void printMessage(String username, String content) {
        lineReader.printAbove("%s : %s".formatted(username, content));
    }

    /**
     * 시스템 메시지를 콘솔에 출력
     */
    public void printSystemMessage(String content) {
        lineReader.printAbove("=> " + content);
    }

    /**
     * 터미널 화면 전체 지우기
     */
    public void clearTerminal() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }
}
