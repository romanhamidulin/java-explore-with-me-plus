package ru.practicum.events.service;

import ru.practicum.events.dto.EventCreateDto;
import ru.practicum.events.dto.EntityParam;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.EventUpdateDto;

import java.util.List;

public interface EventService {
    List<EventShortDto> getEventsByOwner(Long userId, Integer from, Integer size);

    EventDto addEvent(Long userId, EventCreateDto eventCreateDto);

    List<EventShortDto> allEvents(EntityParam params, String ip);

    EventDto getEventByOwner(Long userId, Long eventId);

    EventDto updateEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto);

    EventDto eventById(Long evenId, String ip);
}
