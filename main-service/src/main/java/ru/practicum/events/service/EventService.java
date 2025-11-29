package ru.practicum.events.service;

import ru.practicum.events.dto.EntityParam;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventShortDto;

import java.util.List;

public interface EventService {
    List<EventShortDto> allEvents(EntityParam params, String ip);

    EventDto eventById(Long evenId, String ip);
}
