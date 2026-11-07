package com.example.htmx.examples.activesearch;

import jakarta.validation.constraints.Size;

/** URLから受け取る検索条件。空欄は全件、文字数は前後空白を除く前に制限する。 */
public class SearchForm {
    @Size(max = 60, message = "検索語は60文字以内で入力してください。")
    private String query = "";

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
