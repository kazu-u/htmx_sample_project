package com.example.htmx.examples.updatingothercontent;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

/** 連絡先をセッションごとに保持し、上限付きの不変スナップショットを返す。 */
@Component
@SessionScope
public class UpdatingOtherContentState {
    private static final int MAX_CONTACTS = 20;

    private final List<Contact> contacts = new ArrayList<>();
    private long nextId = 1;
    private long revision = 1;

    public synchronized Snapshot snapshot() {
        return new Snapshot(contacts, MAX_CONTACTS, revision);
    }

    public synchronized Snapshot snapshotForRevision(String expectedRevision) {
        requireRevision(expectedRevision);
        return snapshot();
    }

    public synchronized AddResult add(String name, String email, String expectedRevision) {
        requireRevision(expectedRevision);
        if (contacts.size() >= MAX_CONTACTS) {
            return new AddResult(false, snapshot());
        }
        contacts.add(new Contact(nextId++, name, email));
        revision++;
        return new AddResult(true, snapshot());
    }

    public synchronized Snapshot reset(String expectedRevision) {
        requireRevision(expectedRevision);
        contacts.clear();
        nextId = 1;
        revision++;
        return snapshot();
    }

    private void requireRevision(String expectedRevision) {
        if (!Long.toString(revision).equals(expectedRevision)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    public record Contact(long id, String name, String email) {}

    public record AddResult(boolean added, Snapshot snapshot) {}

    public record Snapshot(List<Contact> contacts, int limit, long revision) {
        public Snapshot {
            contacts = List.copyOf(contacts);
        }
    }
}
