package com.example.htmx.examples.accessiblecombobox;

import jakarta.validation.constraints.Size;

/** 候補検索と選択に使うURL入力。候補IDは固定データと照合する。 */
public class ComboboxForm {
    @Size(max = 60, message = "検索語は60文字以内で入力してください。")
    private String query = "";

    @Size(max = 40, message = "候補を選び直してください。")
    private String contactId = "";

    @Size(max = 40, message = "候補を選び直してください。")
    private String contactIdFallback = "";

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getContactId() { return contactId; }
    public void setContactId(String contactId) { this.contactId = contactId; }
    public String getContactIdFallback() { return contactIdFallback; }
    public void setContactIdFallback(String contactIdFallback) { this.contactIdFallback = contactIdFallback; }
}
