package ru.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class ViewStats {
    private String app;
    private String uri;
    private Long hits;
}
