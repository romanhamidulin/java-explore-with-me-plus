package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.request.dto.ConfirmedRequests;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    @Query("""
            SELECT new ru.practicum.request.dto.ConfirmedRequests(COUNT(DISTINCT r.id), r.event.id)
            FROM Request AS r
            WHERE r.event.id IN (:ids)
            AND r.status = :status
            GROUP BY r.event
            """)
    List<ConfirmedRequests> findAllByEventIdInAndStatus(@Param("ids") List<Long> ids,
                                                        @Param("status") RequestStatus status);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByIdIn(List<Long> ids);

    List<Request> findAllByRequesterId(Long requesterId);

    Optional<Request> findByIdAndRequesterId(Long requestId, Long requesterId);

    Boolean existsByRequesterIdAndEventId(Long userId, Long eventId);
}
