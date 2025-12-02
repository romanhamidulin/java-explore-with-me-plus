package ru.practicum.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.events.dto.*;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.mapper.LocationMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventState;
import ru.practicum.events.model.Location;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.LocationRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.EventConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.dto.ConfirmedRequests;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;


import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.events.model.EventSort;
import ru.practicum.events.model.QEvent;
import ru.practicum.request.model.QRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final EventRepository repository;
    private final StatClient client;

    @Override
    public List<EventShortDto> getEventsByOwner(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).toList();
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmedRequestsMap = requestRepository.findAllByEventIdInAndStatus(eventIds, RequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(ConfirmedRequests::getEvent, ConfirmedRequests::getCount));

        return events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    @Override
    public EventDto addEvent(Long userId, EventCreateDto eventCreateDto) {
        log.info("Валидация даты и времени события");
        validateEventDate(eventCreateDto.getEventDate());

        Event event = EventMapper.toEvent(eventCreateDto);

        log.info("Добавление инициатора события");
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с данным id не найден")
        );
        event.setInitiator(user);

        log.info("Добавление категории события");
        Category category = categoryRepository.findById(eventCreateDto.getCategory()).orElseThrow(
                () -> new NotFoundException("Категория с данным id не найдена")
        );
        event.setCategory(category);

        log.info("Добавление локации события");
        Location location = getOrSaveLocation(eventCreateDto.getLocation());
        event.setLocation(location);

        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        event = eventRepository.save(event);
        EventDto res = EventMapper.toEventDto(event);
        res.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        return res;
    }

    @Override
    public EventDto getEventByOwner(Long userId, Long eventId) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к просмотру данным пользователем")
        );
        EventDto res = EventMapper.toEventDto(event);
        res.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        return res;
    }

    @Override
    public EventDto updateEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto) {
        if (eventId == null || userId == null) {
            throw new ValidationException("Id должен быть указан");
        }
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(
                () -> new NotFoundException("Событие или пользователь с данным id не найдены, или событие недоступно к редактированию данным пользователем")
        );
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new EventConflictException("Изменить можно только отмененные события или события в состоянии ожидания модерации");
        }
        if (eventUpdateDto.getEventDate() != null) {
            log.info("Валидация новых даты и времени события");
            validateEventDate(eventUpdateDto.getEventDate());
            log.info("Обновление даты и времени события");
            event.setEventDate(eventUpdateDto.getEventDate());
        }
        if (eventUpdateDto.getTitle() != null) {
            log.info("Обновление заголовка события");
            event.setTitle(eventUpdateDto.getTitle());
        }
        if (eventUpdateDto.getAnnotation() != null) {
            log.info("Обновление аннотации события");
            event.setAnnotation(eventUpdateDto.getAnnotation());
        }
        if (eventUpdateDto.getDescription() != null) {
            log.info("Обновление описания события");
            event.setDescription(eventUpdateDto.getDescription());
        }
        if (eventUpdateDto.getPaid() != null) {
            log.info("Обновление флага платности события");
            event.setPaid(eventUpdateDto.getPaid());
        }
        if (eventUpdateDto.getRequestModeration() != null) {
            log.info("Обновление признака премодерации заявок на участие");
            event.setRequestModeration(eventUpdateDto.getRequestModeration());
        }
        if (eventUpdateDto.getParticipantLimit() != null) {
            log.info("Обновление лимита пользователей собятия");
            event.setParticipantLimit(eventUpdateDto.getParticipantLimit());
        }

        if (eventUpdateDto.getCategory() != null) {
            log.info("Обновление категории события");
            Category category = categoryRepository.findById(eventUpdateDto.getCategory()).orElseThrow(
                    () -> new NotFoundException("Категория с данным id не найдена")
            );
            event.setCategory(category);
        }
        if (eventUpdateDto.getLocation() != null) {
            log.info("Обновление локации события");
            Location location = getOrSaveLocation(eventUpdateDto.getLocation());
            event.setLocation(location);
        }
        if (eventUpdateDto.getStateAction() != null) {
            switch (eventUpdateDto.getStateAction()) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }
        event = eventRepository.save(event);
        EventDto res = EventMapper.toEventDto(event);
        res.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        return res;
    }

    private void validateEventDate(LocalDateTime eventDate) {
        LocalDateTime minValidDate = LocalDateTime.now().plusHours(2);
        if (eventDate.isBefore(minValidDate)) {
            throw new EventConflictException("Дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента");
        }
    }

    private Location getOrSaveLocation(LocationDto dto) {
        Location location = LocationMapper.toLocation(dto);
        return locationRepository.findByLatAndLon(location.getLat(), location.getLon()).orElseGet(
                () -> locationRepository.save(location)
        );
    }


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
        Integer countOfConfirmedInt = requestRepository.findAllByStatusAndEvent_Id(RequestStatus.CONFIRMED, eventId).size();
        Long countOfConfirmed = countOfConfirmedInt.longValue();
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

    @Override
    public List<EventDto> getEvents(List<Long> users, List<String> states, List<Long> categories,
                                    String rangeStart, String rangeEnd, Integer from, Integer size) {
        log.info("Поиск событий с параметрами: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
                users, states, categories, rangeStart, rangeEnd, from, size);

        validateSearchParameters(users, states, rangeStart, rangeEnd);

        List<EventState> eventStates = parseEventStates(states);
        LocalDateTime start = parseDateTime(rangeStart);
        LocalDateTime end = parseDateTime(rangeEnd);
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<Event> events = eventRepository.findEventsByAdminFilters(users, eventStates, categories, start, end, pageable);

        log.info("Найдено {} событий", events.size());

        return events.stream()
                .map(EventMapper::toEventDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventDto updateEvent(Long eventId, EventAdminUpdateDto updateRequest) {
        log.info("Обновление события с id = {} администратором: {}", eventId, updateRequest);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено"));

        validateEventForAdminUpdate(event, updateRequest);

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            processAdminStateAction(event, updateRequest.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с id = {} успешно обновлено администратором", eventId);

        return EventMapper.toEventDto(updatedEvent);

    }

    private void validateSearchParameters(List<Long> users, List<String> states,
                                          String rangeStart, String rangeEnd) {
        if (rangeStart != null && rangeEnd != null) {
            LocalDateTime start = parseDateTime(rangeStart);
            LocalDateTime end = parseDateTime(rangeEnd);
            if (start.isAfter(end)) {
                throw new ValidationException("Дата начала rangeStart не может быть позже даты окончания rangeEnd");
            }
        }

        if (users != null && !users.isEmpty()) {
            List<Long> existingUsers = userRepository.findExistingUserIds(users);
            if (existingUsers.size() != users.size()) {
                throw new NotFoundException("Некоторые пользователи не найдены");
            }
        }
    }

    private List<EventState> parseEventStates(List<String> states) {
        if (states == null || states.isEmpty()) {
            return null;
        }

        return states.stream()
                .map(stateStr -> {
                    try {
                        return EventState.valueOf(stateStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Некорректное состояние события: " + stateStr);
                    }
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Некорректный формат даты: " + dateTimeStr +
                    ". Ожидается формат: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateEventForAdminUpdate(Event event, EventAdminUpdateDto updateRequest) {
        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = /*parseDateTime(*/ updateRequest.getEventDate(); //);
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Дата начала изменяемого события должна быть не ранее чем за час от текущего времени");
            }
        }

        if (updateRequest.getStateAction() == AdminUpdateStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Событие можно публиковать только, если оно в состоянии ожидания публикации. Текущее состояние: " + event.getState());
            }
        }

        if (updateRequest.getStateAction() == AdminUpdateStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Событие можно отклонить только, если оно еще не опубликовано");
            }
        }
    }

    private void updateEventFields(Event event, EventAdminUpdateDto updateRequest) {
        if (updateRequest.getAnnotation() != null && !updateRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id = " + updateRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (updateRequest.getDescription() != null && !updateRequest.getDescription().isBlank()) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getLocation() != null) {
            Location location = new Location();
            location.setLat(updateRequest.getLocation().getLat());
            location.setLon(updateRequest.getLocation().getLon());
            event.setLocation(location);
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void processAdminStateAction(Event event, AdminUpdateStateAction stateAction) {
        switch (stateAction) {
            case PUBLISH_EVENT:
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                log.info("Событие с id = {} опубликовано", event.getId());
                break;

            case REJECT_EVENT:
                event.setState(EventState.CANCELED);
                log.info("Событие с id = {} отклонено администратором", event.getId());
                break;

            default:
                throw new ValidationException("Некорректное действие: " + stateAction);
        }
    }
}
