package com.example.htmx.examples.urldriventable;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** URL から受け取る絞り込み・並べ替え・ページ指定。 */
public class OrderTableQuery {
    @NotNull
    @Size(max = 60, message = "検索語は60文字以内で入力してください。")
    private String keyword = "";

    @NotNull
    @Pattern(regexp = "ALL|NEW|PREPARING|SHIPPED|DELIVERED", message = "配送状態を選び直してください。")
    private String status = "ALL";

    @NotNull
    @Pattern(regexp = "date|number|customer", message = "並べ替え項目が正しくありません。")
    private String sort = "date";

    @NotNull
    @Pattern(regexp = "asc|desc", message = "並べ替え順が正しくありません。")
    private String direction = "desc";

    @Min(value = 1, message = "ページ番号は1以上を指定してください。")
    private int page = 1;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
}
