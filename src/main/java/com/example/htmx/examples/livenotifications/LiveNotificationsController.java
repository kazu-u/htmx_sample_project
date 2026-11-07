package com.example.htmx.examples.livenotifications;

import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** ライブ通知のページと、読み取り専用のSSEストリームを提供する。 */
@Controller
@RequestMapping("/examples/live-notifications")
public class LiveNotificationsController {
    private final LiveNotificationStream notificationStream;

    public LiveNotificationsController(LiveNotificationStream notificationStream) {
        this.notificationStream = notificationStream;
    }

    @GetMapping
    String page() {
        return "examples/live-notifications";
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> stream(
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId,
            @RequestParam(name = "lastEventId", required = false) String fallbackEventId) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "event-stream", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .body(notificationStream.open(lastEventId == null ? fallbackEventId : lastEventId));
    }
}
