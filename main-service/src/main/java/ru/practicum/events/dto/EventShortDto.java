package ru.practicum.events.dto;

import lombok.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.user.dto.UserShortDto;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class EventShortDto {
    private Long id;
    private String title;
    private String annotation;

    private Long confirmedRequests;
    private Long views;

    private boolean paid;

    private CategoryDto category;

    private UserShortDto initiator;

    private String eventDate;
}
