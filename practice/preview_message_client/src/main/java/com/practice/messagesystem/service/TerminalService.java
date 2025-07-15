package com.practice.messagesystem.service;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.nio.file.Paths;

//콘솔에서 입력을 받고 출력하는 역할을 하는 클래스
public class TerminalService {

    private Terminal terminal;
    private LineReader lineReader;

    private TerminalService() {}

    //객체 생성 정적 팩토리 메서드
    public static TerminalService create() throws IOException {
        TerminalService terminalService = new TerminalService();
        try {
            terminalService.terminal = TerminalBuilder.builder().system(true).build();
        } catch (IOException ex) {
            System.err.println("Failed to create TerminalService. error: " + ex.getMessage());
            throw ex;
        }

        //이전 입력 기록 저장 설정
        terminalService.lineReader =
                LineReaderBuilder.builder()
                        .terminal(terminalService.terminal)
                        .variable(LineReader.HISTORY_FILE, Paths.get("./data/history.txt"))
                        .build();

        return terminalService;
    }

    //사용자로부터 한 줄 입력받기
    public String readLine(String prompt) {
        String input = lineReader.readLine(prompt);
        terminal.puts(InfoCmp.Capability.cursor_up);
        terminal.puts(InfoCmp.Capability.delete_line);
        terminal.flush();

        return input;
    }

    //일반 메시지 출력
    public void printMessage(String username, String content) {
        lineReader.printAbove(String.format("%s : %s", username, content));
    }

    //시스템 메시지 출력
    public void printSystemMessage(String content) {
        lineReader.printAbove("=> " + content);
    }

    //콘솔 전체 비우기
    public void clearTerminal() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }
}
