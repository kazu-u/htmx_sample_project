package com.example.htmx.examples.resetuserinput;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** メモ送信時の入力とサーバー検証を表す。 */
public class NoteForm {
    @NotBlank(message = "メモを入力してください。")
    @Size(max = 120, message = "メモは120文字以内で入力してください。")
    private String text = "";

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
