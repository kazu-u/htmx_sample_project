package com.example.htmx.examples.clicktoedit;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

/** セッションごとに連絡先を1件保持する。排他と版の照合で古いフォームの上書きを防ぐ。 */
@Component
@SessionScope
public class ContactState {
    private Contact contact = initialContact();

    public synchronized Contact snapshot() {
        return contact;
    }

    public synchronized void save(ContactForm form) {
        requireCurrentRevision(form.getRevision());
        contact = new Contact(form.getName(), form.getEmail(), UUID.randomUUID().toString());
    }

    public synchronized void requireCurrentRevision(String revision) {
        requireRevision(revision);
    }

    public synchronized void reset(String revision) {
        requireRevision(revision);
        contact = initialContact();
    }

    private void requireRevision(String revision) {
        if (!contact.revision().equals(revision)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    private static Contact initialContact() {
        return new Contact("山田 花子", "hanako@example.com", UUID.randomUUID().toString());
    }

    /** 変更できないスナップショットを返し、テンプレート描画中の値の混在を防ぐ。 */
    public record Contact(String name, String email, String revision) {}
}
