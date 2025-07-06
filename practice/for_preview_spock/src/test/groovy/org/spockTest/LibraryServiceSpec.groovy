package org.spockTest

import spock.lang.Specification

class LibraryServiceSpec extends Specification {

    def "도서 이용 가능 여부를 확인한다"() {
        given: // [테스트 준비 블록]: 테스트에 필요한 데이터들을 정의하는 블록

        //가짜 객체(Stub) 생성
        def bookRepository = Stub(BookRepository)
        PushService pushService = Stub()

        //가짜 객체의 동작 설정
        // bookRepository.findBookByIsbn(_)는 어떤 문자열이 오든 (_ = any)
        bookRepository.findBookByIsbn(_ as String) >> Optional.of(new Book("1234", "Stub", true)) //_는 any와 같은 의미라고 보면 된다

        //테스트 대상 클래스 생성
        def libraryService = new LibraryService(bookRepository, pushService)

        when: // [실행 블록]: 어떤 행위를 실행하는 블록
        //테스트할 메서드 실행
        def isBookAvailable = libraryService.isBookAvailable("1234")

        then: // [결과 검증 블록]
        isBookAvailable
    }

    def "대출 요청 시 도서 상태에 따른 처리 결과를 확인한다"() {

        given:
        //가짜 객체(Stub) 생성
        def bookRepository = Stub(BookRepository)
        PushService pushService = Stub()

        //가짜 객체의 동작 설정
        bookRepository.findBookByIsbn(_ as String) >> {
            bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty()
        }

        //테스트 대상 클래스 생성
        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def borrowBook = libraryService.borrowBook(isbn)

        then:
        expected == borrowBook

        where: //[스트 메서드를 다양한 입력 값과 기대 값으로 반복 실행할 수 있도록 해줌]
        bookExists | isbn   | title        | available || expected
        true       | "1234" | "bookTitle1" | true      || Optional.of("bookTitle1")
        true       | "5678" | "bookTitle2" | false     || Optional.empty()
        false      | "9999" | "Not used"   | true      || Optional.empty()
    }

    def "대출에 성공하면 알림이 발송되어야 한다"(){

        given:
        //가짜 객체(Stub) 생성
        def bookRepository = Mock(BookRepository)
        PushService pushServiceMock = Mock()

        //가짜 객체의 동작 설정
//        bookRepository.findBookByIsbn(_ as String) >> Optional.of(new Book("1234", "bookName", true))

        //테스트 대상 클래스 생성
        def libraryService = new LibraryService(bookRepository, pushServiceMock)

        when:
        def borrowBook = libraryService.borrowBook("1234")

        then:
        Optional.of("bookName") == borrowBook
        1 * pushServiceMock.notification(_ as String)
        1 * bookRepository.findBookByIsbn(_ as String) >> Optional.of(new Book("1234", "bookName", true))
    }

    def "도서 조회중에 예외가 발생하면 대촐 요청 시 예외를 던진다"(){
        given:
        //가짜 객체(Stub) 생성
        def bookRepository = Stub(BookRepository)
        PushService pushService = Stub()

        //가짜 객체의 동작 설정
        bookRepository.findBookByIsbn(_ as String) >> {
            throw new RuntimeException("Database error")
        }

        //테스트 대상 클래스 생성
        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def borrowBook = libraryService.borrowBook("1234")

        then:
        thrown(RuntimeException)
    }

    def "Spy 테스트"(){
        given:

        def bookRepository = Stub(BookRepository)
        PushService pushService = Stub()

        bookRepository.findBookByIsbn(_ as String) >> Optional.of(new Book("1234", "Stub", true))

        LibraryService libraryService = Spy(constructorArgs: [bookRepository, pushService]){
            borrowBook(_ as String) >> Optional.of("Overridden Spy")
        }

        when:
        def borrowBook = libraryService.borrowBook("1234")
        def isBookAvailable = libraryService.isBookAvailable("1234")

        then:
        isBookAvailable
        Optional.of("Overridden Spy") == borrowBook

    }
}
