package com.example.htmx.examples.deleterow;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

/** セッションごとの架空データを保持し、削除と初期化を同期して行う。 */
@Component
@SessionScope
public class DeleteRowState {
    private static final List<Task> INITIAL_TASKS = List.of(
            new Task(1, "画面の表示確認", "佐藤 花子", "未着手"),
            new Task(2, "入力フォームの整理", "鈴木 太郎", "対応中"),
            new Task(3, "テスト項目の見直し", "高橋 美咲", "未着手"),
            new Task(4, "説明文の更新", "田中 健", "完了")
    );

    private final List<Task> tasks = new ArrayList<>(INITIAL_TASKS);

    public synchronized List<Task> snapshot() {
        return List.copyOf(tasks);
    }

    public synchronized void delete(int id) {
        boolean removed = tasks.removeIf(task -> task.id() == id);
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public synchronized void reset() {
        tasks.clear();
        tasks.addAll(INITIAL_TASKS);
    }

    /** 変更できない一覧要素。利用者入力や HTML は含めない。 */
    public record Task(int id, String title, String owner, String status) {}
}
