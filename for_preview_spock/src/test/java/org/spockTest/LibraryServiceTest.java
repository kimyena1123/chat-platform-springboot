package org.spockTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LibraryServiceTest {


    @Test
    @DisplayName("도서 이용 가능 여부를 확인한다")
    void isBookAvailable() {
        //1. 가짜 객체(mock) 만들기
        BookRepository bookRepository = mock(BookRepository.class);
        PushService pushService = mock(PushService.class);

        //2. 가짜 객체의 동작 지정
        when(bookRepository.findBookByIsbn(anyString())).thenReturn(Optional.of(new Book("1234", "Stub", true)));

        //3. 테스트 대상 클래스 생성
        LibraryService libraryService = new LibraryService(bookRepository, pushService);

        //4. 실제 테스트할 메서드 호출
        boolean bookAvailable = libraryService.isBookAvailable("1234");

        assertTrue(bookAvailable); //위 메서드가 true를 반환해야 테스트 성공
    }


    public static Stream<Arguments> borrowBookDataProvider() {
        //boolean bookExists, String isbn, String title, boolean available, Optional<String> expected
        return Stream.of(
                Arguments.of(true, "1234", "bookTitle1", true, Optional.of("bookTitle1")),
                Arguments.of(true, "5678", "bookTitle2", false, Optional.empty()),
                Arguments.of(false, "9999", "Not used", true, Optional.empty())
        );
    }

    @ParameterizedTest(name = "bookExists={0},isbn={1},title={2},available={3} => expected={4}")
    @MethodSource("borrowBookDataProvider")
    @DisplayName("대출 요청 시 도서 상태에 따른 처리 결과를 확인한다")
     void borrowBook(boolean bookExists, String isbn, String title, boolean available, Optional<String> expected) {
        //1. 가짜 객체(mock) 만들기
        BookRepository bookRepository = mock(BookRepository.class);
        PushService pushService = mock(PushService.class);

        //2. 가짜 객체의 동작 지정
        when(bookRepository.findBookByIsbn(anyString())).thenReturn(bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty());

        //3. 테스트 대상 클래스 생성
        LibraryService libraryService = new LibraryService(bookRepository, pushService);

        //4. 실제 테스트할 메서드 호출\
        Optional<String> borrowedBook = libraryService.borrowBook(isbn);

        assertEquals(expected, borrowedBook);
    }
}