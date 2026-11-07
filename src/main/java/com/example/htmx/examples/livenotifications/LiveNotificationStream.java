package com.example.htmx.examples.livenotifications;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;

/** 架空通知を有界なSSE接続で配信し、完了・切断時に予約処理を解放する。 */
@Service
public class LiveNotificationStream {
    static final int MAX_ACTIVE_STREAMS = 8;
    static final int MAX_NOTIFICATIONS = 5;
    static final Duration EVENT_INTERVAL = Duration.ofSeconds(3);
    static final Duration STREAM_TIMEOUT = Duration.ofSeconds(20);

    private static final Logger logger = LoggerFactory.getLogger(LiveNotificationStream.class);
    private static final DateTimeFormatter RECEIVED_AT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);
    private static final List<Notification> NOTIFICATIONS = List.of(
            new Notification("注文", "注文番号 ORD-1042 を受け付けました。", "information"),
            new Notification("在庫", "架空商品「青空ノート」の在庫が少なくなっています。", "warning"),
            new Notification("配送", "注文番号 ORD-1038 を出荷しました。", "information"),
            new Notification("在庫", "架空商品「森のペン」の補充を確認しました。", "information"),
            new Notification("注文", "注文番号 ORD-1043 の確認が必要です。", "warning"));

    private final TaskScheduler scheduler;
    private final TemplateEngine templateEngine;
    private final Semaphore permits = new Semaphore(MAX_ACTIVE_STREAMS);
    private final Set<StreamSession> activeStreams = ConcurrentHashMap.newKeySet();

    public LiveNotificationStream(
            @Qualifier("liveNotificationTaskScheduler") TaskScheduler scheduler,
            TemplateEngine templateEngine) {
        this.scheduler = scheduler;
        this.templateEngine = templateEngine;
    }

    SseEmitter open(String lastEventId) {
        int lastSentId = parseLastEventId(lastEventId);
        if (!permits.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The maximum number of live notification streams has been reached.");
        }

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
        StreamSession stream = new StreamSession(emitter, lastSentId);
        activeStreams.add(stream);
        emitter.onCompletion(stream::release);
        emitter.onTimeout(() -> stream.fail(new TimeoutException("Live notification stream timed out.")));
        emitter.onError(error -> stream.release());

        try {
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                    stream::sendNext,
                    Instant.now().plus(lastSentId == MAX_NOTIFICATIONS ? Duration.ZERO : EVENT_INTERVAL),
                    EVENT_INTERVAL);
            stream.setFuture(future);
        } catch (RejectedExecutionException exception) {
            stream.fail(exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The live notification scheduler is unavailable.", exception);
        }
        return emitter;
    }

    int activeStreamCount() {
        return activeStreams.size();
    }

    void closeActiveStreams() {
        activeStreams.forEach(StreamSession::complete);
    }

    @PreDestroy
    void stopStreamsOnShutdown() {
        closeActiveStreams();
    }

    private int parseLastEventId(String lastEventId) {
        if (lastEventId == null) {
            return 0;
        }
        if (lastEventId.length() == 1
                && lastEventId.charAt(0) >= '0'
                && lastEventId.charAt(0) <= (char) ('0' + MAX_NOTIFICATIONS)) {
            return lastEventId.charAt(0) - '0';
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Last-Event-ID.");
    }

    private String render(Notification notification) {
        Context context = new Context(Locale.JAPAN);
        context.setVariable("notification", new NotificationView(
                notification.category(),
                notification.message(),
                notification.severity(),
                RECEIVED_AT.format(LocalTime.now())));
        return templateEngine.process("fragments/live-notification", Set.of("notificationItem"), context);
    }

    private record Notification(String category, String message, String severity) {}

    private record NotificationView(String category, String message, String severity, String receivedAt) {}

    private final class StreamSession {
        private final SseEmitter emitter;
        private final AtomicBoolean closed = new AtomicBoolean();
        private int nextNotificationIndex;
        private volatile ScheduledFuture<?> future;

        private StreamSession(SseEmitter emitter, int lastSentId) {
            this.emitter = emitter;
            this.nextNotificationIndex = lastSentId;
        }

        private void setFuture(ScheduledFuture<?> scheduledFuture) {
            future = scheduledFuture;
            if (closed.get()) {
                scheduledFuture.cancel(false);
            }
        }

        private void sendNext() {
            if (closed.get()) {
                return;
            }
            try {
                if (nextNotificationIndex == NOTIFICATIONS.size()) {
                    finishNormally();
                    return;
                }

                int eventId = nextNotificationIndex + 1;
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .id(Integer.toString(eventId))
                        .data(render(NOTIFICATIONS.get(nextNotificationIndex)), TEXT_HTML_UTF8));
                nextNotificationIndex++;
                if (nextNotificationIndex == NOTIFICATIONS.size()) {
                    finishNormally();
                }
            } catch (IOException exception) {
                logger.debug("Live notification client disconnected before the stream completed.");
                fail(exception);
            } catch (TemplateEngineException exception) {
                logger.error("Could not render a live notification.", exception);
                fail(exception);
            }
        }

        private void finishNormally() throws IOException {
            emitter.send(SseEmitter.event().name("finished").data("finished"));
            complete();
        }

        private void complete() {
            if (!closeState()) {
                return;
            }
            emitter.complete();
        }

        private void fail(Throwable error) {
            if (!closeState()) {
                return;
            }
            emitter.completeWithError(error);
        }

        private void release() {
            closeState();
        }

        private boolean closeState() {
            if (!closed.compareAndSet(false, true)) {
                return false;
            }
            ScheduledFuture<?> scheduledFuture = future;
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            activeStreams.remove(this);
            permits.release();
            return true;
        }
    }
}
