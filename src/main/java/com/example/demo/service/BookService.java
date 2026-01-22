package com.example.demo.service;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import com.example.demo.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service layer for managing books.
 * Handles business logic for adding books from Google Books API.
 */
@Service
public class BookService {
    
    private final BookRepository bookRepository;
    private final GoogleBookService googleBookService;
    private final BookMapper bookMapper;

    @Autowired
    public BookService(BookRepository bookRepository, GoogleBookService googleBookService, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.googleBookService = googleBookService;
        this.bookMapper = bookMapper;
    }

    /**
     * Adds a book from Google Books API to the personal list.
     * 
     * @param googleId the Google Books volume ID
     * @return the persisted Book entity
     * @throws IllegalArgumentException if the book is not found or volumeInfo is null
     */
    public Book addBookFromGoogle(String googleId) {
        // Fetch book from Google Books API
        GoogleBook googleBook = googleBookService.searchBooks(googleId, 10, 0);

        // Find the specific book by ID from the items
        GoogleBook.VolumeInfo volumeInfo = extractVolumeInfo(googleBook, googleId);

        if (volumeInfo == null) {
            throw new IllegalArgumentException("Book with Google ID: " + googleId + " not found");
        }

        // Map using dedicated mapper
        Book book = bookMapper.toBook(googleId, volumeInfo);
        
        // Persist and return
        return bookRepository.save(book);
    }

    /**
     * Extracts VolumeInfo for a specific Google ID from the search results.
     * 
     * @param googleBook the Google Books API response
     * @param googleId the Google Books volume ID to find
     * @return the VolumeInfo if found, null otherwise
     */
    private GoogleBook.VolumeInfo extractVolumeInfo(GoogleBook googleBook, String googleId) {
        if (googleBook == null || googleBook.items() == null) {
            return null;
        }

        return googleBook.items().stream()
                .filter(item -> item.id().equals(googleId))
                .map(GoogleBook.Item::volumeInfo)
                .findFirst()
                .orElse(null);
    }
}