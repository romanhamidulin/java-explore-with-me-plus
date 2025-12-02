package ru.practicum.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.events.dto.EventAdminUpdateDto;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.service.EventService;
import ru.practicum.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
public class EventAdminController {
    private final EventService eventAdminService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventDto> getEvents(@RequestParam(required = false) List<Long> users,
                                    @RequestParam(required = false) List<String> states,
                                    @RequestParam(required = false) List<Long> categories,
                                    @RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart, // ← ВАЖНО!
                                    @RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                    @RequestParam(defaultValue = "0") Integer from,
                                    @RequestParam(defaultValue = "10") Integer size) {

        return eventAdminService.getEvents(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventDto updateEventAdmin(@PathVariable Long eventId,
                                     @Valid @RequestBody EventAdminUpdateDto updateRequest,
                                     BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> String.format("Поле: %s. Ошибка: %s. Значение: %s",
                            error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                    .collect(Collectors.joining("; "));

            log.error("Ошибки валидации: {}", errorMessage);
            throw new ValidationException("Некорректные данные в запросе: " + errorMessage);
        }

        log.info("PATCH /admin/events/{} получен запрос: {}", eventId, updateRequest);

        try {
            EventDto result = eventAdminService.updateEvent(eventId, updateRequest);
            log.info("PATCH /admin/events/{} успешно обработан: {}", eventId, result);
            return result;
        } catch (Exception e) {
            log.error("Ошибка при обработке PATCH /admin/events/{}: {}", eventId, e.getMessage(), e);
            throw e;
        }
    }
}
