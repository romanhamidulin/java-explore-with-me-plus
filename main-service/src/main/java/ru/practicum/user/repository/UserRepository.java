package ru.practicum.user.repository;

import ru.practicum.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllById(List<Long> ids, Pageable pageable);
}
