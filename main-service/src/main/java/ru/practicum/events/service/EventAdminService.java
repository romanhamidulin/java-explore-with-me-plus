package ru.practicum.events.service;

import ru.practicum.events.dto.EventAdminUpdateDto;
import ru.practicum.events.dto.EventDto;

import java.util.List;

public interface EventAdminService {
    List<EventDto> getEvents(List<Long> users, List<String> states, List<Long> categories,
                             String rangeStart, String rangeEnd, Integer from, Integer size);

    EventDto updateEvent(Long eventId, EventAdminUpdateDto updateRequest);
}
