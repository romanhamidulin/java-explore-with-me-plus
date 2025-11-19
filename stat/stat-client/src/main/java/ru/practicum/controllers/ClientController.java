package ru.practicum.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.service.ClientService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> hitNewStats(@RequestBody @Valid EndpointHitDto dto) {
        return clientService.hit(dto);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStats>> getNewStats(@RequestParam(name = "start") LocalDateTime start,
                                                       @RequestParam(name = "end") LocalDateTime end,
                                                       @RequestParam(name = "uris", required = false) List<String> uris,
                                                       @RequestParam(name = "unique", required = false) Boolean unique) {
        return clientService.getStats(start, end, uris, unique);
    }

}
