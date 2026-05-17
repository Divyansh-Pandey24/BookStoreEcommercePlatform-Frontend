package com.booknest.ebook.repository;

import com.booknest.ebook.entity.EBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EBookRepository extends JpaRepository<EBook, Long> {
    List<EBook> findByActiveTrue();
}
