package com.study.integrationtest.controller;

import com.study.integrationtest.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
//@RequiredArgsConstructor //lombok 주입하면 사용 가능. 그게 아니라면 생성자 생성
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/books/{isbn}/availability")
    public ResponseEntity<String> isBookAvailable(@PathVariable String isbn) {
        boolean bookAvailable = libraryService.isBookAvailable(isbn);

        return ResponseEntity.ok(String.format("%s : %s", isbn, bookAvailable ? "대출가능" : "대출불가능"));
    }

    @PostMapping("/books/{isbn}/borrow")
    public ResponseEntity<String> borrowBook(@PathVariable String isbn){

        return libraryService
                .borrowBook(isbn)
                .map(title -> ResponseEntity.ok(String.format("%s : %s", isbn, title)))
                .orElse(ResponseEntity.ok(String.format("%s : 대출불가능", isbn)));
    }
}
