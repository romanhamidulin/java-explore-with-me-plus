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
    @NotBlank
    private String app;
    @NotBlank
    private String uri;
    @NotBlank
    private Long hits;
}
