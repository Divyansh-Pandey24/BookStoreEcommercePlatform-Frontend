package com.booknest.book;

import com.booknest.book.repository.BookSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BookService1ApplicationTests {

	@MockBean
	BookSearchRepository bookSearchRepository;

	@Test
	void contextLoads() {
		// Verifies the Spring context loads without a live Elasticsearch or other external services
	}

}
