package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class BookControllerTests {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private BookRepository bookRepository;
    
    static MockWebServer server;

    @BeforeEach
    void setup() throws IOException {
        bookRepository.deleteAll();
        bookRepository.save(new Book("lRtdEAAAQBAJ", "Spring in Action", "Craig Walls"));
        bookRepository.save(new Book("existing123", "Existing Book", "Existing Author"));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }
    
    @Test
    void testGetAllBooks() throws Exception {
        mockMvc.perform(get("/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Spring in Action"))
            .andExpect(jsonPath("$[0].author").value("Craig Walls"))
            .andExpect(jsonPath("$[1].title").value("Existing Book"));
    }

    @Test
    void testAddBookFromGoogle_validId_returns201_andPersistsBook() throws Exception {
        startMockServer();
        String mockResponse = Files.readString(Paths.get("src", "test", "resources", "effectivejava.json"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        // Act: POST /books/ka2VUBqHiWkC
        mockMvc.perform(post("/books/ka2VUBqHiWkC"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("ka2VUBqHiWkC"))
            .andExpect(jsonPath("$.title").value("Effective Java"))
            .andExpect(jsonPath("$.author").value("Joshua Bloch"))
            .andExpect(jsonPath("$.pageCount").value(375));

        // Verify: Book is persisted in database
        Book savedBook = bookRepository.findById(1L).orElse(null);
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.getTitle()).isEqualTo("Effective Java");
        assertThat(savedBook.getAuthor()).isEqualTo("Joshua Bloch");
        assertThat(savedBook.getPageCount()).isEqualTo(375);
    }

    @Test
    void testAddBookFromGoogle_mapsAllFields() throws Exception {
        startMockServer();
        String mockResponse = Files.readString(Paths.get("src", "test", "resources", "effectivejava.json"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        mockMvc.perform(post("/books/ka2VUBqHiWkC"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("ka2VUBqHiWkC"))
            .andExpect(jsonPath("$.title").value("Effective Java"))
            .andExpect(jsonPath("$.author").value("Joshua Bloch"));
    }

    @Test
    void testAddBookFromGoogle_invalidId_returns400_nothingPersisted() throws Exception {
        startMockServer();
        String mockResponse = Files.readString(Paths.get("src", "test", "resources", "effectivejava.json"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        int initialCount = bookRepository.findAll().size();

        // Act: POST with ID that doesn't exist in Google response
        mockMvc.perform(post("/books/nonexistent123"))
            .andExpect(status().isBadRequest());

        // Verify: Nothing was persisted
        assertThat(bookRepository.findAll()).hasSize(initialCount);
    }

    @Test
    void testAddBookFromGoogle_emptyResponse_returns400() throws Exception {
        startMockServer();
        String emptyResponse = "{\"kind\": \"books#volumes\", \"totalItems\": 0, \"items\": []}";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(emptyResponse));

        int initialCount = bookRepository.findAll().size();

        mockMvc.perform(post("/books/anyId"))
            .andExpect(status().isBadRequest());

        assertThat(bookRepository.findAll()).hasSize(initialCount);
    }

    @Test
    void testSearchGoogleBooks_returnsGoogleSchema() throws Exception {
        startMockServer();
        String mockResponse = Files.readString(Paths.get("src", "test", "resources", "effectivejava.json"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        mockMvc.perform(get("/google?q=effective+java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kind").value("books#volumes"))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].volumeInfo.title").value("Effective Java"));
    }

    private static void startMockServer() throws IOException {
        if (server == null) {
            server = new MockWebServer();
            server.start();
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) throws IOException {
        startMockServer();
        registry.add("google.books.base-url", () -> server.url("/").toString());
    }
}