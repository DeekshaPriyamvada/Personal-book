package com.example.demo.mapper;

import com.example.demo.db.Book;
import com.example.demo.google.GoogleBook;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert GoogleBook data to Book entity.
 * Handles all data transformation and validation logic.
 */
@Component
public class BookMapper {

    /**
     * Maps Google Book VolumeInfo to a Book entity.
     * 
     * @param googleId the Google Books volume ID
     * @param volumeInfo the volume information from Google Books API
     * @return a Book entity ready to be persisted
     * @throws IllegalArgumentException if volumeInfo is null or required fields are missing
     */
    public Book toBook(String googleId, GoogleBook.VolumeInfo volumeInfo) {
        validateVolumeInfo(volumeInfo);

        Book book = new Book();
        book.setId(googleId);
        book.setTitle(getTitle(volumeInfo));
        book.setAuthor(getFirstAuthor(volumeInfo));
        book.setPageCount(getPageCount(volumeInfo));

        return book;
    }

    /**
     * Extracts the title from VolumeInfo.
     * Returns empty string if title is null or blank.
     * 
     * @param volumeInfo the volume information
     * @return the title or empty string
     */
    private String getTitle(GoogleBook.VolumeInfo volumeInfo) {
        String title = volumeInfo.title();
        return title != null && !title.isBlank() ? title : "";
    }

    /**
     * Extracts the first author from the authors list.
     * Returns null if no authors are available.
     * 
     * @param volumeInfo the volume information
     * @return the first author or null
     */
    private String getFirstAuthor(GoogleBook.VolumeInfo volumeInfo) {
        if (volumeInfo.authors() == null || volumeInfo.authors().isEmpty()) {
            return null;
        }
        
        String firstAuthor = volumeInfo.authors().get(0);
        return firstAuthor != null && !firstAuthor.isBlank() ? firstAuthor : null;
    }

    /**
     * Extracts and validates the page count.
     * Returns null if page count is null, zero, or negative.
     * 
     * @param volumeInfo the volume information
     * @return the page count or null
     */
    private Integer getPageCount(GoogleBook.VolumeInfo volumeInfo) {
        Integer pageCount = volumeInfo.pageCount();
        
        if (pageCount == null || pageCount <= 0) {
            return null;
        }
        
        return pageCount;
    }

    /**
     * Validates that the VolumeInfo contains essential data.
     * 
     * @param volumeInfo the volume information to validate
     * @throws IllegalArgumentException if volumeInfo is null or lacks required fields
     */
    private void validateVolumeInfo(GoogleBook.VolumeInfo volumeInfo) {
        if (volumeInfo == null) {
            throw new IllegalArgumentException("VolumeInfo cannot be null");
        }

        if (volumeInfo.title() == null || volumeInfo.title().isBlank()) {
            throw new IllegalArgumentException("Book title is required");
        }
    }
}