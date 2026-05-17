package com.booknest.book.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

// Elasticsearch document representing a book for search indexing
@Document(indexName = "books", createIndex = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDocument {

    // Unique identifier in Elasticsearch (matches MySQL bookId)
    @Id
    private String bookId;

    // Title field optimized for full-text search
    @Field(type = FieldType.Text)
    private String title;

    // Author field optimized for full-text search
    @Field(type = FieldType.Text)
    private String author;

    // Genre field for exact match filtering
    @Field(type = FieldType.Keyword)
    private String genre;

    // Publisher field for exact match filtering
    @Field(type = FieldType.Keyword)
    private String publisher;

    // Numeric field for price range filtering
    @Field(type = FieldType.Double)
    private Double price;

    // Numeric field for stock level tracking
    @Field(type = FieldType.Integer)
    private Integer stock;

    // Average rating for search ranking
    @Field(type = FieldType.Double)
    private Double rating;

    // Description text for full-text search
    @Field(type = FieldType.Text)
    private String description;

    // URL to the cover image
    private String coverImageUrl;

    // Status flags
    private Boolean featured;
    private Boolean active;
}