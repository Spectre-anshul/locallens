package com.locallens.event.controller;

import com.locallens.event.model.EventDocument;
import com.locallens.event.service.EventAggregatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventAggregatorService eventService;

    @GetMapping("/tonight")
    public ResponseEntity<List<EventDocument>> getTonightEvents(@RequestParam String city) {
        return ResponseEntity.ok(eventService.getTonightEvents(city));
    }
}
