package ru.practicum.model;

import org.springframework.stereotype.Component;
import ru.practicum.dto.EndpointHitDto;

@Component
public class StatisticMapper {
    public Statistic toStatistic(EndpointHitDto dto) {
        Statistic stat = new Statistic();
        stat.setApp(dto.getApp());
        stat.setUri(dto.getUri());
        stat.setIp(dto.getIp());
        stat.setRequestDate(dto.getTimestamp());
        return stat;
    }
}
