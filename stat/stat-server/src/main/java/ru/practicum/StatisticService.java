package ru.practicum;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticService {
    void saveStatistic(EndpointHitDto dto);

    List<ViewStats> getStatistics(LocalDateTime start,
                                  LocalDateTime end,
                                  List<String> uris,
                                  Boolean unique);
}
