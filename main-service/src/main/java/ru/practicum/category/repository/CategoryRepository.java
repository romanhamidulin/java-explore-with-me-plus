package ru.practicum.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.category.model.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c FROM Category c " +
            "ORDER BY  c.id " +
            "OFFSET ?1 " +
            "FETCH NEXT ?2 ROWS ONLY")
    List<Category> findAllByIdBetween(long from, long size);
}
