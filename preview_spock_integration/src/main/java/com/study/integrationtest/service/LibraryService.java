package com.study.integrationtest.service;

import com.study.integrationtest.entity.Book;
import com.study.integrationtest.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LibraryService {

    private final BookRepository bookRepository;
    private final PushService pushService;

    // 생성자 주입 (의존성 주입)
    public LibraryService(BookRepository bookRepository, PushService pushService) {
        this.bookRepository = bookRepository;
        this.pushService = pushService;
    }

    // ====== 책이 대출 가능한지 확인하는 기능 ======
    public boolean isBookAvailable(String isbn){
        Optional<Book> book = bookRepository.findBookByIsbn(isbn); // 책 조회를 먼저 하고(책을 ISBN으로 찾고)

        // 책이 존재하면 이용 가능 여부를 리턴, 없으면 false
        return book.map( it -> it.isAvailable()).orElse(false); // 책이 이용가능한지 boolean 값으로 받고, 조회 안되면 false 리턴
    }

    // ====== 책을 대출하는 기능(대출 성공하면 대출한 책의 제목을 리턴) ======
    //borrowBook: 대출 진행 + Kafka 알림 전송
    public Optional<String> borrowBook(String isbn){

        return bookRepository.findBookByIsbn(isbn)
                .filter(Book::isAvailable) // 이용 가능한 책만 처리
                .map(book -> {
                    // 알림 보내기 (대출 완료)
                    pushService.notification("대출 완료: " + book.getTitle());
                    return book.getTitle();
                });
    }

}
