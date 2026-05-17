package com.booknest.cart.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * This is a copy of the important fields from
 * Book Service's BookResponse.
 *
 * WHY do we need this here?
 * When Feign calls Book Service and gets a response,
 * it needs to deserialize (convert) that JSON into
 * a Java object.
 *
 * We only copy the fields we actually USE in Cart Service:
 * title, price, stock, coverImageUrl.
 * We don't need publishedDate, description etc.
 *
 * Jackson (the JSON library) only maps fields that exist.
 * Extra fields in the JSON response are simply ignored.
 * So this works perfectly even though Book Service
 * returns more fields.
 */
@Data
@NoArgsConstructor
public class BookResponse {
    private Long    bookId;
    private String  title;
    private Double  price;
    private Integer stock;
    private String  coverImageUrl;
    private Boolean inStock;
    private Boolean active;
}