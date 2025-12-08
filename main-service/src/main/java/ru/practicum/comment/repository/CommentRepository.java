package ru.practicum.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus commentStatus);

    List<Comment> findAllByEventIdInAndStatus(List<Long> idsList, CommentStatus commentStatus);

    List<Comment> findAllByStatus(CommentStatus commentStatus);

    List<Comment> findByEventIdInAndStatus(List<Long> eventIds, CommentStatus commentStatus);
}