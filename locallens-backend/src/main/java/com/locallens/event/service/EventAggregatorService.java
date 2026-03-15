package com.locallens.event.service;

import com.locallens.event.model.EventDocument;
import com.locallens.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventAggregatorService {

    private final EventRepository eventRepository;

    public List<EventDocument> getEventsForItinerary(String city, LocalDate start, LocalDate end) {
        Instant startInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return eventRepository.findByLocationCityAndStartTimeBetween(city, startInstant, endInstant);
    }

    public List<EventDocument> getTonightEvents(String city) {
        Instant now = Instant.now();
        Instant endOfDay = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return eventRepository.findByLocationCityAndStartTimeBetween(city, now, endOfDay);
    }

    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    public void nightlyEventAggregation() {
        log.info("Running nightly event aggregation...");
        // TODO: Pull from Eventbrite API, deduplicate, store
    }

    @Scheduled(fixedRate = 3600000) // hourly
    public void realTimeEventRefresh() {
        log.info("Refreshing real-time events...");
        // TODO: Refresh event status, tickets remaining
    }
}
