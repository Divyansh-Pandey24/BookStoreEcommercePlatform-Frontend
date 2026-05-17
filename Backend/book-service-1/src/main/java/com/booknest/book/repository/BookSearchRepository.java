package com.booknest.book.repository;

import com.booknest.book.document.BookDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// Repository for Elasticsearch operations on BookDocument
@Repository
public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {

    // Filter active books by genre
    List<BookDocument> findByGenreAndActiveTrue(String genre);

    // Filter active featured books
    List<BookDocument> findByFeaturedTrueAndActiveTrue();

    // Filter active books by price range
    List<BookDocument> findByPriceBetweenAndActiveTrue(Double minPrice, Double maxPrice);

    // Search active books using fuzzy matching on multiple fields
    @Query("{\"bool\": {" +
           "\"must\": [{\"term\": {\"active\": true}}]," +
           "\"should\": [" +
           "{\"match\": {\"title\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}," +
           "{\"match\": {\"author\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}," +
           "{\"match\": {\"description\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}" +
           "]," +
           "\"minimum_should_match\": 1" +
           "}}")
    List<BookDocument> fuzzySearch(String keyword);
}