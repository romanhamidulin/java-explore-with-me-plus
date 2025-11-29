package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

import java.util.List;
import java.util.Map;

public interface RequestRepository extends JpaRepository<Request, Long>, QuerydslPredicateExecutor<Request> {
    List<Request> findAllByStatusAndEvent_Id(RequestStatus status, Long eventId);

    @Query("SELECT r.event.id, COUNT(r) FROM Request r WHERE r.status = ?1 AND r.event.id IN ?2 GROUP BY r.event.id")
    Map<Long, Long> countConfirmedRequestsByEvents(RequestStatus status, List<Long> eventIds);
}
