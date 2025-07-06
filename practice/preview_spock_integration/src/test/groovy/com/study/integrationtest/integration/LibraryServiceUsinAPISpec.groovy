package com.study.integrationtest.integration

import com.study.integrationtest.entity.Book
import com.study.integrationtest.repository.BookRepository
import com.study.integrationtest.service.LibraryService
import com.study.integrationtest.service.PushService
import org.spockframework.spring.SpringBean
import org.spockframework.spring.StubBeans
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
@StubBeans([PushService])
class LibraryServiceUsinAPISpec extends Specification {

    @Autowired
    private MockMvc mockMvc

    @SpringBean
    private BookRepository bookRepository = Stub()

//    PushService pushService = Stub()

    def "도서 이용 가능 여부를 확인한다"() {
        given:
//        def bookRepository = Stub(BookRepository)
        bookRepository.findBookByIsbn(_ as String) >> Optional.of(new Book("1234", "Stub", true))

        when:
        def resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/books/1234/availability"))

        then:
        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("1234 : 대출가능"))

    }

    def "대출 요청 시 도서 상태에 따른 처리 결과를 확인한다"() {

        given:
//        def bookRepository = Stub(BookRepository)
        bookRepository.findBookByIsbn(_ as String) >> {
            bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty()
        }

        when:
        def resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/books/${isbn}/borrow"))

        then:
        resultActions
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus))
                .andExpect(MockMvcResultMatchers.content().string(expectedBody))


        where:
        bookExists | isbn   | title        | available | expectedStatus || expectedBody
        true       | "1234" | "Spock"      | true      | 200            || "1234 : Spock"
        true       | "5678" | "bookTitle2" | false     | 200            || "5678 : 대출불가능"
        false      | "9999" | "Not used"   | true      | 200            || "9999 : 대출불가능"
    }

}
