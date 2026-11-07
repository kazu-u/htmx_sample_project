package com.example.htmx.examples.bulkupdate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** チェックされたIDだけを受け取る。未送信の行は無効として扱う。 */
public class BulkUpdateForm {
    @Size(max = 4, message = "選択できる行は4件までです。")
    private List<@Pattern(regexp = "[1-4]", message = "対象の行を確認してください。") String> activeIds = new ArrayList<>();

    @NotBlank(message = "版情報がありません。再読み込みしてください。")
    @Size(min = 36, max = 36, message = "版情報が不正です。再読み込みしてください。")
    private String revision;

    public List<String> getActiveIds() { return activeIds; }
    public void setActiveIds(List<String> activeIds) { this.activeIds = activeIds; }
    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }
}
