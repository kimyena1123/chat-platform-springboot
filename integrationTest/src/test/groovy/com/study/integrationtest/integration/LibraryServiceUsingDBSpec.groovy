package com.study.integrationtest.integration

import com.study.integrationtest.entity.Book
import com.study.integrationtest.repository.BookRepository
import com.study.integrationtest.service.LibraryService
import com.study.integrationtest.service.PushService
import org.spockframework.spring.EnableSharedInjection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import spock.lang.Shared
import spock.lang.Specification

@DataJpaTest
@EnableSharedInjection
class LibraryServiceUsingDBSpec extends Specification {

    @Autowired
    @Shared
    private BookRepository bookRepository;

    PushService pushService = Stub()

    //test 코드가 실행되기 전에 setup 함수가 먼저 실행된다
    def setupSpec(){
        bookRepository.save(new Book("1234", "Spock", true))
        bookRepository.save(new Book("5678", "Spring", false))
        bookRepository.flush()

    }

    def "도서 이용 가능 여부를 확인한다"() {
        given:
        /* [BookRepository를 Stub으로 잡았던 걸 제거하고 실제 DB로 연동]
            - 실제 DB가 어디 있는가? 실제 DB는 테스트 실행 중 생성되는 임시 메모리 데이터베이스(H2 등)이다
            - 실제 데이터베이스 서버 (MySQL, PostgreSQL 등) 는 아니고, Spring Boot가 테스트를 위해 자동으로 띄운 임시 메모리 DB(H2)이다
            - 디스크에 저장되는 게 아니라, 메모리에서만 동작하는 일시적인 DB
         */
//        def bookRepository = Stub(BookRepository)
//        bookRepository.findBookByIsbn(_ as String) >> Optional.of(new Book("1234", "Stub", true)) //_는 any와 같은 의미라고 보면 된다

        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def isBookAvailable = libraryService.isBookAvailable("1234")

        then:
        isBookAvailable
    }

    def "대출 요청 시 도서 상태에 따른 처리 결과를 확인한다"() {

        given:
        // BookRepository를 Stub으로 잡았던 걸 제거하고 실제 DB로 연동
//        def bookRepository = Stub(BookRepository)
//        bookRepository.findBookByIsbn(_ as String) >> {
//            bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty()
//        }

        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def borrowBook = libraryService.borrowBook(isbn)

        then:
        expected == borrowBook

        where: //title과 available은 db에 있기에 제거
        isbn   || expected
        "1234" || Optional.of("Spock")
        "5678" || Optional.empty()
        "9999" || Optional.empty()
    }

}
