package ru.practicum.user.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
public class UserRequest {
    private List<Long> ids;
    private Integer from;
    private Integer size;

    public UserRequest(List<Long> ids, Integer from, Integer size) {
        this.ids = ids;
        this.from = from;
        this.size = size;
    }
}
