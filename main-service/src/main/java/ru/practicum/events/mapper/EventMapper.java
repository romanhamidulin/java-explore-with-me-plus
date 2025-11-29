package ru.practicum.events.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.LocationDto;
import ru.practicum.events.model.Event;
import ru.practicum.user.dto.UserShortDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventMapper {

    public static EventDto mapToDto(Event event, Integer confirmedRequest, Long views) {
        return EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .confirmedRequests(confirmedRequest)
                .views(views)
                .paid(event.isPaid())
                .requestModeration(event.isRequestModeration())
                .participantLimit(event.getParticipantLimit())
                .location(LocationDto.builder().
                        lat(event.getLocation().getLat())
                        .lon(event.getLocation().getLat())
                        .build())
                .state(event.getState())
                .initiator(UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .eventDate(event.getEventDate().toString())
                .createdOn(event.getCreatedOn().toString())
                .publishedOn(event.getPublishedOn().toString())
                .build();
    }

    public static EventShortDto mapToShortDto(Event event, Long confirmedRequest, Long views) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .confirmedRequests(confirmedRequest)
                .views(views)
                .paid(event.isPaid())
                .category(CategoryDto.builder()
                        .id(event.getCategory().getId())
                        .name(event.getCategory().getName())
                        .build())
                .initiator(UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .eventDate(event.getEventDate().toString())
                .build();
    }
}
