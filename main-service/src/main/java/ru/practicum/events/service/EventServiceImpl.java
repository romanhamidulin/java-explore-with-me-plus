package ru.practicum.events.service;


import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.events.dto.EntityParam;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventSort;
import ru.practicum.events.model.EventState;
import ru.practicum.events.model.QEvent;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.model.QRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository repository;
    private final RequestRepository requestRepository;
    private final StatClient client;

    @Override
    public List<EventShortDto> allEvents(EntityParam params, String ip) {

        if (params.getRangeStart() != null && params.getRangeEnd() != null && params.getRangeStart().isAfter(params.getRangeEnd())) {
            throw new ValidationException("Дата начала не может идти после даты конца");
        }

        BooleanExpression expression = prepareAndBuildQuery(params);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        List<Event> events = repository.findAll(expression, pageable).getContent();

        List<EventShortDto> shortDtos = buildEvents(events);

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("main-service")
                .ip(ip)
                .uri("/events")
                .timestamp(LocalDateTime.now())
                .build();
        client.hit(hitDto);

        if (params.getSort() == EventSort.EVENT_DATE) {
            return shortDtos.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).toList().reversed();
        }

        return shortDtos.stream().sorted(Comparator.comparing(EventShortDto::getViews)).toList().reversed();
    }

    @Override
    public EventDto eventById(Long eventId, String ip) {
        Event event = checkEvent(eventId);
        Integer countOfConfirmed = requestRepository.findAllByStatusAndEvent_Id(RequestStatus.CONFIRMED, eventId).size();
        Long countOfViews = getViews(eventId);

        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("main-service")
                .ip(ip)
                .uri("/events/" + eventId)
                .timestamp(LocalDateTime.now())
                .build();
        client.hit(hitDto);

        return EventMapper.mapToDto(event, countOfConfirmed, countOfViews);
    }

    private Event checkEvent(Long eventID) {
        return repository.findById(eventID).orElseThrow(() -> new NotFoundException("Событие с id=" + eventID + "не найден"));
    }

    private Long getViews(Long eventId) {
        LocalDateTime start = LocalDateTime.now().minusYears(2);
        LocalDateTime end = LocalDateTime.now();
        List<String> uri = List.of("/events/" + eventId);

        List<ViewStats> statResponse = client.getStats(start, end, uri, true).getBody();
        if (statResponse != null && !statResponse.isEmpty()) {
            return statResponse.getFirst().getHits();
        }

        return 0L;
    }

    private BooleanExpression prepareAndBuildQuery(EntityParam param) {
        QEvent event = QEvent.event;
        BooleanExpression predicate = event.state.eq(EventState.PUBLISHED);

        if (param.getText() != null && !param.getText().isEmpty()) {
            predicate.and(event.annotation.likeIgnoreCase(param.getText()).or(event.description.likeIgnoreCase(param.getText())));
        }

        if (param.getCategories() != null && !param.getCategories().isEmpty()) {
            predicate.and(event.category.id.in(param.getCategories()));
        }

        if (param.getPaid() != null) {
            predicate.and(event.paid.eq(param.getPaid()));
        }

        if (param.getRangeStart() != null && param.getRangeEnd() != null) {
            predicate.and(event.eventDate.between(param.getRangeStart(), param.getRangeEnd()));
        } else {
            predicate.and(event.eventDate.after(LocalDateTime.now()));
        }


        if (param.getOnlyAvailable() != null && param.getOnlyAvailable()) {
            QRequest request = QRequest.request;

            Expression<Integer> confirmedRequestsCount = JPAExpressions
                    .select(request.count().intValue())
                    .from(request)
                    .where(request.event.id.eq(event.id)
                            .and(request.status.eq(RequestStatus.CONFIRMED)));

            predicate.and(event.participantLimit.gt(confirmedRequestsCount));
        }
        return predicate;
    }

    private List<EventShortDto> buildEvents(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedRequests = requestRepository.countConfirmedRequestsByEvents(RequestStatus.CONFIRMED, eventIds);

        Map<Long, Long> views = getViewsForEvents(eventIds);


        return events.stream().map(event -> {
            Long confirmedR = confirmedRequests.get(event.getId());
            Long view = views.get(event.getId());
            return EventMapper.mapToShortDto(event, confirmedR, view);
        }).toList();
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        LocalDateTime start = LocalDateTime.now().minusYears(2);
        LocalDateTime end = LocalDateTime.now();

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        List<ViewStats> statResponse = client.getStats(start, end, uris, true).getBody();
        Map<Long, Long> viewsMap = new HashMap<>();

        if (statResponse != null) {
            for (ViewStats stats : statResponse) {
                String uri = stats.getUri();
                Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
                viewsMap.put(eventId, stats.getHits());
            }
        }

        return viewsMap;
    }
}
