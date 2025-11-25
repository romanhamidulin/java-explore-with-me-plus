package ru.practicum.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ParticipationRequestDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private Long event;

    private Long requester;

    private String created;

    private String status;
}
