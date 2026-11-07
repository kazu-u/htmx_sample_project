package com.example.htmx.examples.livenotifications;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** アプリ全体の接続上限とストリーム後始末を確認する。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LiveNotificationStreamTests {
    @Autowired
    LiveNotificationStream stream;

    @LocalServerPort
    int port;

    @Autowired
    @Qualifier("liveNotificationTaskScheduler")
    ThreadPoolTaskScheduler scheduler;

    @AfterEach
    void closeStreams() {
        stream.closeActiveStreams();
    }

    @Test
    void activeStreamsAreBoundedAndReleasesCapacityWhenClosed() {
        for (int i = 0; i < LiveNotificationStream.MAX_ACTIVE_STREAMS; i++) {
            stream.open(null);
        }
        assertThat(stream.activeStreamCount()).isEqualTo(LiveNotificationStream.MAX_ACTIVE_STREAMS);

        assertThatThrownBy(() -> stream.open(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        stream.closeActiveStreams();
        assertThat(stream.activeStreamCount()).isZero();
        stream.open(null);
        assertThat(stream.activeStreamCount()).isEqualTo(1);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void disconnectedClientReleasesStreamAfterNextWriteFails() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/examples/live-notifications/stream"))
                .GET()
                .build();

        HttpResponse<InputStream> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofInputStream());

        assertThat(response.statusCode()).isEqualTo(200);
        try (InputStream body = response.body();
                BufferedReader events = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            assertThat(events.readLine()).isEqualTo("event:notification");
        }

        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (stream.activeStreamCount() > 0 && System.nanoTime() < deadline) {
            Thread.sleep(100);
        }
        assertThat(stream.activeStreamCount()).isZero();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void timedOutStreamReleasesConnectionCapacity() throws Exception {
        CountDownLatch schedulerStarted = new CountDownLatch(LiveNotificationStream.MAX_ACTIVE_STREAMS);
        CountDownLatch releaseScheduler = new CountDownLatch(1);
        List<ScheduledFuture<?>> blockers = new ArrayList<>();
        try {
            for (int i = 0; i < LiveNotificationStream.MAX_ACTIVE_STREAMS; i++) {
                blockers.add(scheduler.schedule(() -> {
                    schedulerStarted.countDown();
                    try {
                        releaseScheduler.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                }, Instant.now()));
            }
            assertThat(schedulerStarted.await(5, TimeUnit.SECONDS)).isTrue();

            long openedAt = System.nanoTime();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://localhost:" + port + "/examples/live-notifications/stream"))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HttpClient.newHttpClient().send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream());

            assertThat(response.statusCode()).isEqualTo(500);
            InputStream responseBody = response.body();
            try {
                long deadline = System.nanoTime() + Duration.ofSeconds(26).toNanos();
                while (stream.activeStreamCount() > 0 && System.nanoTime() < deadline) {
                    Thread.sleep(100);
                }
            } finally {
                responseBody.close();
            }

            long elapsedMillis = Duration.ofNanos(System.nanoTime() - openedAt).toMillis();
            assertThat(stream.activeStreamCount()).isZero();
            assertThat(elapsedMillis).isGreaterThanOrEqualTo(19_000);
        } finally {
            releaseScheduler.countDown();
            blockers.forEach(blocker -> blocker.cancel(true));
        }
    }
}
