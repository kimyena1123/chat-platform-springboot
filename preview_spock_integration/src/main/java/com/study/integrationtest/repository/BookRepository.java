package com.study.integrationtest.repository;

import com.study.integrationtest.entity.Book;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//DB 접근을 위한 인터페이스
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // 책의 ISBN을 통해 Book을 찾기 위한 메서드 (JPA가 자동으로 구현해줌)
    // SELECT * FROM book WHERE isbn = ?
    Optional<Book> findBookByIsbn(String isbn);
}
