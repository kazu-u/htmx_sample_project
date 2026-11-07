package com.example.htmx.examples.progressbar;

import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

/** セッション内に高々1件の模擬ジョブを保持し、時刻から進捗を計算する。 */
@Component
@SessionScope
public class ProgressJobState {
    static final Duration DURATION = Duration.ofSeconds(10);
    private Job job;

    public synchronized Snapshot snapshot(Instant now) {
        if (job == null) {
            return new Snapshot(Status.IDLE, 0, "まだ開始していません。");
        }
        int progress = progressAt(job.cancelledAt() == null ? now : job.cancelledAt());
        if (job.cancelledAt() != null) {
            return new Snapshot(Status.CANCELLED, progress, "処理をキャンセルしました。");
        }
        if (progress == 100) {
            return job.outcome() == Outcome.SUCCESS
                    ? new Snapshot(Status.COMPLETED, progress, "処理が完了しました。")
                    : new Snapshot(Status.FAILED, progress, "模擬エラーにより処理に失敗しました。");
        }
        return new Snapshot(Status.RUNNING, progress, "処理中です。");
    }

    public synchronized Snapshot start(Outcome outcome, Instant now) {
        if (snapshot(now).status().running()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        job = new Job(outcome, now, null);
        return snapshot(now);
    }

    public synchronized Snapshot cancel(Instant now) {
        if (!snapshot(now).status().running()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        job = new Job(job.outcome(), job.startedAt(), now);
        return snapshot(now);
    }

    private int progressAt(Instant now) {
        long elapsedMillis = Math.max(0, Duration.between(job.startedAt(), now).toMillis());
        return (int) Math.min(100, elapsedMillis * 100 / DURATION.toMillis());
    }

    public enum Outcome {
        SUCCESS,
        FAILURE
    }

    public enum Status {
        IDLE,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED;

        public boolean running() {
            return this == RUNNING;
        }
    }

    public record Snapshot(Status status, int progress, String message) {}

    private record Job(Outcome outcome, Instant startedAt, Instant cancelledAt) {}
}
