package ru.practicum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.exception.ClientError;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClientService {
    private final RestClient restClient;
    @Value("${stats-server.url:http://localhost:9090}")
    private String statUrl;

    public ClientService() {
        restClient = RestClient.builder()
                .baseUrl(statUrl)
                .build();
    }

    public ResponseEntity<String> hit(EndpointHitDto dto) {

        ResponseEntity<EndpointHitDto> result = restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toEntity(EndpointHitDto.class);

        if (result.getStatusCode() == HttpStatus.CREATED) {
            return ResponseEntity.ok("Информация сохранена");
        } else {
            throw new ClientError("Произошла ошибка с записью данных");
        }
    }

    public ResponseEntity<List<ViewStats>> getStats(LocalDateTime start, LocalDateTime end,
                                                    List<String> uris, Boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(statUrl + "/stats")
                .queryParam("start", start)
                .queryParam("end", end);
        if (uris != null && uris.isEmpty()) {
            builder.queryParam("uris", uris.toArray());
        }

        if (unique != null) {
            builder.queryParam("unique", unique);
        }

        List<ViewStats> result = restClient.get()
                .uri(builder.build().toUriString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return ResponseEntity.ok(result);
    }


}
