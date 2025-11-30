package ru.practicum.events.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import ru.practicum.exception.EventConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.request.dto.ConfirmedRequests;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
}
