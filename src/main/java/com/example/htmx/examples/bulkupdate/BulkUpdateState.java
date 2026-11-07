package com.example.htmx.examples.bulkupdate;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

/** セッション内の4人を保持し、全行の更新と版照合を同じ排他区間で行う。 */
@Component
@SessionScope
public class BulkUpdateState {
    private List<Person> people = initialPeople();
    private String revision = UUID.randomUUID().toString();

    public synchronized Snapshot snapshot() {
        return new Snapshot(people, revision);
    }

    public synchronized void requireCurrentRevision(String expected) {
        if (!revision.equals(expected)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    public synchronized Snapshot save(Set<String> activeIds, String expected) {
        requireCurrentRevision(expected);
        if (!Set.of("1", "2", "3", "4").containsAll(activeIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        people = people.stream().map(person -> new Person(person.id(), person.name(),
                person.email(), activeIds.contains(Integer.toString(person.id())))).toList();
        revision = UUID.randomUUID().toString();
        return snapshot();
    }

    public synchronized void reset(String expected) {
        requireCurrentRevision(expected);
        people = initialPeople();
        revision = UUID.randomUUID().toString();
    }

    private static List<Person> initialPeople() {
        return List.of(
                new Person(1, "青空 花子", "hanako@example.com", true),
                new Person(2, "若葉 太郎", "taro@example.com", false),
                new Person(3, "朝日 葵", "aoi@example.com", true),
                new Person(4, "夕凪 蓮", "ren@example.com", false));
    }

    public record Person(int id, String name, String email, boolean active) {}
    public record Snapshot(List<Person> people, String revision) {}
}
