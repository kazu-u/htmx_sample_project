package com.example.htmx.examples.valueselect;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 固定の架空地域を使い、親子の選択肢と組み合わせを参照・検証する。 */
@Controller
@RequestMapping("/examples/value-select")
public class ValueSelectController {
    private static final List<Prefecture> PREFECTURES = List.of(
            new Prefecture("aoba", "青葉県", List.of(
                    new City("aoba-city", "青葉市"), new City("wakaba-city", "若葉市"),
                    new City("sakuragawa-city", "桜川市"))),
            new Prefecture("hoshimi", "星見県", List.of(
                    new City("hoshimi-city", "星見市"), new City("tenkuu-city", "天空市"),
                    new City("tsukikage-town", "月影町"))),
            new Prefecture("asanagi", "朝凪県", List.of(
                    new City("asanagi-city", "朝凪市"), new City("umikaze-town", "海風町"),
                    new City("hinata-village", "日向村")))
    );

    @GetMapping
    String page(@RequestParam(required = false) String prefectureId,
                @RequestParam(required = false) String cityId,
                @RequestParam(required = false) String action,
                @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        String effectiveAction = htmx && action == null ? "confirm" : action;
        Prefecture prefecture = findPrefecture(prefectureId);
        City city = prefecture == null ? null : findCity(prefecture, cityId);
        String prefectureError = null;
        String cityError = null;
        Selection selection = null;
        String selectionError = null;

        if ("loadCities".equals(effectiveAction) && prefecture == null) {
            prefectureError = "都道府県を選択してください。";
            selectionError = "都道府県を選択してから候補を表示してください。";
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
        } else if ("confirm".equals(effectiveAction)) {
            if (prefecture == null) {
                prefectureError = "都道府県を選択してください。";
                selectionError = prefectureError;
            } else if (city == null) {
                cityError = cityId == null || cityId.isBlank()
                        ? "市区町村を選択してください。"
                        : "選択した市区町村は、この都道府県に属していません。";
                selectionError = cityError;
            } else {
                selection = new Selection(prefecture.name(), city.name());
            }
            if (selection == null) {
                response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
            }
        }

        addPageModel(model, prefecture, city, prefectureError, cityError, selection, selectionError);
        if (htmx && "confirm".equals(effectiveAction)) {
            return "examples/value-select-result :: selectionResponse";
        }
        if (htmx && "loadCities".equals(effectiveAction)) {
            return "examples/value-select-result :: loadCitiesResponse";
        }
        return "examples/value-select";
    }

    /** 親 ID が固定データにある場合だけ、その子候補をHTML断片で返す。 */
    @GetMapping("/cities")
    String cities(@RequestParam(required = false) String prefectureId,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response) {
        response.addHeader("Vary", "HX-Request");
        Prefecture prefecture = findPrefecture(prefectureId);
        String prefectureError = null;
        if (prefectureId != null && !prefectureId.isBlank() && prefecture == null) {
            prefectureError = "選択内容を読み直してください。";
            response.setStatus(HttpStatus.UNPROCESSABLE_CONTENT.value());
        }
        String cityError = prefectureError == null ? null : "都道府県を選び直してください。";
        addPageModel(model, prefecture, null, prefectureError, cityError, null, null);
        return htmx ? "examples/value-select-city :: cityResponse" : "examples/value-select";
    }

    private void addPageModel(Model model, Prefecture prefecture, City city,
                              String prefectureError, String cityError,
                              Selection selection, String selectionError) {
        model.addAttribute("prefectures", PREFECTURES);
        model.addAttribute("selectedPrefectureId", prefecture == null ? "" : prefecture.id());
        model.addAttribute("cities", prefecture == null ? List.of() : prefecture.cities());
        model.addAttribute("selectedCityId", city == null ? "" : city.id());
        model.addAttribute("prefectureError", prefectureError);
        model.addAttribute("cityError", cityError);
        model.addAttribute("selection", selection);
        model.addAttribute("selectionError", selectionError);
    }

    private Prefecture findPrefecture(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return PREFECTURES.stream().filter(prefecture -> prefecture.id().equals(id))
                .findFirst().orElse(null);
    }

    private City findCity(Prefecture prefecture, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return prefecture.cities().stream().filter(city -> city.id().equals(id))
                .findFirst().orElse(null);
    }

    public record Prefecture(String id, String name, List<City> cities) {}

    public record City(String id, String name) {}

    public record Selection(String prefectureName, String cityName) {}
}
