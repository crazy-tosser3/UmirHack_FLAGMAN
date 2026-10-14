package com.example.UmirHack_Flagman.events;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/")
public class EventsHandler {
    private final SseBroadcaster broadcaster;

    public EventsHandler(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(value = "events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        return broadcaster.subscribe();
    }
}
