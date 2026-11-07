package com.example.htmx.examples.inlinevalidation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

/** メール単独検証と送信時の検証で同じ制約を使う。検証結果は保存しない。 */
public class ValidationForm {
    public interface EmailCheck {}

    @NotBlank(message = "名前を入力してください。")
    @Size(max = 60, message = "名前は60文字以内で入力してください。")
    private String name;

    @NotBlank(message = "メールアドレスを入力してください。", groups = {Default.class, EmailCheck.class})
    @Email(message = "メールアドレスの形式を確認してください。", groups = {Default.class, EmailCheck.class})
    @Size(max = 254, message = "メールアドレスは254文字以内で入力してください。",
            groups = {Default.class, EmailCheck.class})
    private String email;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
