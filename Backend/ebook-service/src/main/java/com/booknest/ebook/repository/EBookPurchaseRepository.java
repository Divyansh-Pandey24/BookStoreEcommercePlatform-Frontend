package com.booknest.ebook.repository;

import com.booknest.ebook.entity.EBookPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EBookPurchaseRepository extends JpaRepository<EBookPurchase, Long> {
    List<EBookPurchase> findByUserId(Long userId);
    Optional<EBookPurchase> findByUserIdAndEbookId(Long userId, Long ebookId);
    boolean existsByUserIdAndEbookId(Long userId, Long ebookId);
    void deleteByEbookId(Long ebookId);
}
