package ru.practicum.user.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
public class UserRequest {
    private List<Long> ids;
    private Integer from = 0;
    private Integer size = 10;
}
