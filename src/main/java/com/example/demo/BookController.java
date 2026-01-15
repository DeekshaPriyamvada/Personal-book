package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
public class BookController {
    private final BookRepository bookRepository;
    private final GoogleBookService googleBookService;

    @Autowired
    public BookController(BookRepository bookRepository, GoogleBookService googleBookService) {
        this.bookRepository = bookRepository;
        this.googleBookService = googleBookService;
    }

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        System.out.println("Fetching all books from the repository");
        return bookRepository.findAll();
    }

    @GetMapping("/google")
    public GoogleBook searchGoogleBooks(@RequestParam("q") String query,
            @RequestParam(value = "maxResults", required = false) Integer maxResults,
            @RequestParam(value = "startIndex", required = false) Integer startIndex) {
        return googleBookService.searchBooks(query, maxResults, startIndex);
    }

    @PostMapping("/books/{googleId}")
    public ResponseEntity<Book> addBookFromGoogle(@PathVariable String googleId) {
        try {
            System.out.println("Fetching book with Google ID: " + googleId);
            // Fetch book from Google Books API using existing searchBooks method
            GoogleBook googleBook = googleBookService.searchBooks(googleId, 10, 0);

            System.out.println("Google Book Response: " + googleBook);

            // Find the specific book by ID from the items
            GoogleBook.VolumeInfo volumeInfo = null;
            if (googleBook != null && googleBook.items() != null) {
                volumeInfo = googleBook.items().stream()
                        .filter(item -> item.id().equals(googleId))
                        .map(GoogleBook.Item::volumeInfo)
                        .findFirst()
                        .orElse(null);
            }

            System.out.println("Volume Info: " + volumeInfo);

            if (volumeInfo == null) {
                System.out.println("Volume Info is null, returning 400 Bad Request");
                return ResponseEntity.badRequest().build();
            }

            // Map and persist
            Book book = mapGoogleBookToBook(googleId, volumeInfo);
            Book savedBook = bookRepository.save(book);

            System.out.println("Book saved successfully: " + savedBook);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
        } catch (Exception e) {
            System.out.println("Exception occurred: " + e.getMessage());
            System.out.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    private Book mapGoogleBookToBook(String googleId, GoogleBook.VolumeInfo volumeInfo) {
        Book book = new Book();

        System.out.println("Mapping Google Book to Book entity for ID: " + googleId);

        book.setId(googleId); // ✅ Fixed: use 'id' not 'googleId'
        book.setTitle(volumeInfo.title());

        // Get first author if available
        if (volumeInfo.authors() != null && !volumeInfo.authors().isEmpty()) {
            book.setAuthor(volumeInfo.authors().get(0));
        }

        if (volumeInfo.pageCount() != null) {
            book.setPageCount(volumeInfo.pageCount());
        }

        System.out.println("Mapped Book: " + book);
        return book;
    }
}
