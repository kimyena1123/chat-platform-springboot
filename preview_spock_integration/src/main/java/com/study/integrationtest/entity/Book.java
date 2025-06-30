package com.study.integrationtest.entity;

import jakarta.persistence.*;

import java.util.Objects;

//이 클래스는 DB의 Book 테이블과 1:1로 연결
// 이 클래스는 데이터베이스 테이블과 매핑됨 (JPA에서 사용하는 어노테이션)
@Entity
public class Book {

    //생성자
    // JPA에서 엔티티는 기본 생성자(아무 매개변수가 없는 생성자)가 꼭 필요하다
    public Book() {}

    //사용할 생성자. 책의 고유번호(isbn), 제목, 이용 가능 여부를 지정해서 Book 객체 생성
    public Book(String isbn, String title, boolean available){
        this.isbn = isbn;
        this.title = title;
        this.available = available;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean available;


    // ======== Getter 메서드 (값을 읽기 위한 메서드) =========
    public Long getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAvailable() {
        return available;
    }

    // ======== toString: 객체를 문자열로 표현할 때 사용 =========
    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", available=" + available +
                '}';
    }

    // ======== equals와 hashCode는 객체 비교에 사용 =========
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Book book = (Book) object;
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
