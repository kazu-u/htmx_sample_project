package com.example.htmx.examples.undoabledelete;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

/** セッション内の削除待ち状態を保持し、期限を含む一覧スナップショットを同期して作る。 */
@Component
@SessionScope
public class UndoableDeleteState {
    static final Duration UNDO_WINDOW = Duration.ofSeconds(30);

    private final List<Task> tasks = new ArrayList<>();
    private long nextRevision = 1;
    private long listRevision = 1;

    public UndoableDeleteState() {
        restoreInitialTasks();
    }

    public synchronized PageSnapshot pageSnapshot(Instant now) {
        List<TaskView> taskViews = tasks.stream().map(task -> view(task, now, null)).toList();
        return new PageSnapshot(taskViews, pageRevision(now));
    }

    public synchronized TaskView requestDeletion(int id, long revision, Instant now) {
        Task task = requireTask(id);
        if (task.revision != revision || status(task, now) != Status.ACTIVE) {
            throw conflict();
        }
        task.pendingSince = now;
        task.revision = allocateRevision();
        listRevision = allocateRevision();
        return view(task, now, "削除待ちにしました。30秒以内に取り消せます。");
    }

    public synchronized TaskView undoDeletion(int id, long revision, Instant now) {
        Task task = requireTask(id);
        if (task.revision != revision || status(task, now) != Status.PENDING_DELETE) {
            throw conflict();
        }
        task.pendingSince = null;
        task.revision = allocateRevision();
        listRevision = allocateRevision();
        return view(task, now, "削除を取り消しました。");
    }

    public synchronized void reset(String expectedPageRevision, Instant now) {
        if (!pageRevision(now).equals(expectedPageRevision)) {
            throw conflict();
        }
        restoreInitialTasks();
        listRevision = allocateRevision();
    }

    private String pageRevision(Instant now) {
        StringBuilder revision = new StringBuilder().append(listRevision);
        for (Task task : tasks) {
            revision.append(':').append(task.id)
                    .append('-').append(task.revision)
                    .append('-').append(status(task, now));
        }
        return revision.toString();
    }

    private void restoreInitialTasks() {
        tasks.clear();
        tasks.add(new Task(1, "議事録を共有する", "佐藤 花子", allocateRevision()));
        tasks.add(new Task(2, "テスト結果を確認する", "鈴木 太郎", allocateRevision()));
        tasks.add(new Task(3, "画面案内を更新する", "高橋 美咲", allocateRevision()));
        tasks.add(new Task(4, "問い合わせに返信する", "田中 健", allocateRevision()));
        tasks.add(new Task(5, "会議室を予約する", "伊藤 葵", allocateRevision()));
    }

    private Task requireTask(int id) {
        return tasks.stream().filter(task -> task.id == id).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Status status(Task task, Instant now) {
        if (task.pendingSince == null) {
            return Status.ACTIVE;
        }
        return now.isBefore(task.pendingSince.plus(UNDO_WINDOW))
                ? Status.PENDING_DELETE : Status.DELETED;
    }

    private TaskView view(Task task, Instant now, String actionMessage) {
        return new TaskView(task.id, task.title, task.owner, status(task, now),
                task.revision, actionMessage);
    }

    private long allocateRevision() {
        return nextRevision++;
    }

    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT);
    }

    public enum Status {
        ACTIVE,
        PENDING_DELETE,
        DELETED
    }

    public record TaskView(int id, String title, String owner, Status status,
                           long revision, String actionMessage) {}

    public record PageSnapshot(List<TaskView> tasks, String revision) {
        public PageSnapshot {
            tasks = List.copyOf(tasks);
        }
    }

    private static final class Task {
        private final int id;
        private final String title;
        private final String owner;
        private long revision;
        private Instant pendingSince;

        private Task(int id, String title, String owner, long revision) {
            this.id = id;
            this.title = title;
            this.owner = owner;
            this.revision = revision;
        }
    }
}
