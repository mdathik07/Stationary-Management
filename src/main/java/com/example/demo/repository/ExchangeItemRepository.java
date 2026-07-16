package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.ExchangeItem;
import com.example.demo.entity.User;

public interface ExchangeItemRepository extends JpaRepository<ExchangeItem, Long> {

    List<ExchangeItem> findBySellerOrderByCreatedAtDesc(User seller);

    @Query("""
            select i from ExchangeItem i
            where i.available = true
              and (:category is null or i.category = :category)
              and (:q is null
                   or lower(i.itemName) like lower(concat('%', :q, '%'))
                   or lower(i.description) like lower(concat('%', :q, '%')))
            order by i.createdAt desc
            """)
    List<ExchangeItem> search(@Param("q") String q, @Param("category") String category);

    @Query("select distinct i.category from ExchangeItem i where i.available = true and i.category is not null order by i.category")
    List<String> findActiveCategories();
}
