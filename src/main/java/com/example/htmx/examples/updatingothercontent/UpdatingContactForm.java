package com.example.htmx.examples.updatingothercontent;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 追加する連絡先の入力値。制約はリクエスト改変時にもサーバーで検証する。 */
public class UpdatingContactForm {
    @NotBlank(message = "名前を入力してください。")
    @Size(max = 60, message = "名前は60文字以内で入力してください。")
    private String name;

    @NotBlank(message = "メールアドレスを入力してください。")
    @Email(message = "メールアドレスの形式を確認してください。")
    @Size(max = 254, message = "メールアドレスは254文字以内で入力してください。")
    private String email;

    private String revision;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }
}
