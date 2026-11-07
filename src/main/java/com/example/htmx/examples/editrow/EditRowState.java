package com.example.htmx.examples.editrow;

import com.example.htmx.examples.clicktoedit.ContactForm;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

/** セッション内の3行を保持し、行の版照合と保存を一つの排他区間で行う。 */
@Component
@SessionScope
public class EditRowState {
    private List<Person> people = initialPeople();
    private String revision = newRevision();

    public synchronized Snapshot snapshot() {
        return new Snapshot(List.copyOf(people), revision);
    }

    public synchronized void requireCurrentRevision(int id, String expectedRevision) {
        var person = find(id);
        if (!person.revision().equals(expectedRevision)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    public synchronized void save(int id, ContactForm form) {
        requireCurrentRevision(id, form.getRevision());
        int index = people.indexOf(find(id));
        people.set(index, new Person(id, form.getName(), form.getEmail(), newRevision()));
        revision = newRevision();
    }

    /** 一覧の版も照合し、別タブで保存した内容を古い初期化要求で消さない。 */
    public synchronized void reset(String expectedRevision) {
        if (!revision.equals(expectedRevision)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        people = initialPeople();
        revision = newRevision();
    }

    private Person find(int id) {
        return people.stream().filter(person -> person.id() == id).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static String newRevision() {
        return UUID.randomUUID().toString();
    }

    private static List<Person> initialPeople() {
        return new ArrayList<>(List.of(
                new Person(1, "青空 花子", "hanako@example.com", newRevision()),
                new Person(2, "若葉 太郎", "taro@example.com", newRevision()),
                new Person(3, "朝日 葵", "aoi@example.com", newRevision())));
    }

    public record Person(int id, String name, String email, String revision) {}
    public record Snapshot(List<Person> people, String revision) {}
}
