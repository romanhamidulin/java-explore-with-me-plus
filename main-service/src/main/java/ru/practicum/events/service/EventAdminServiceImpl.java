package ru.practicum.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.events.dto.AdminUpdateStateAction;
import ru.practicum.events.dto.EventAdminUpdateDto;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventState;
import ru.practicum.events.model.Location;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventAdminServiceImpl implements EventAdminService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

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

