package com.example.htmx.examples.fileupload;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 入力エラー時にフォームを再描画し、ファイル入力だけは htmx で保持する。 */
@Controller
@RequestMapping("/examples/file-upload-input")
class FileUploadInputController {
    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final int PAYLOAD_TOO_LARGE = 413;

    @GetMapping
    String index() {
        return "examples/file-upload-input";
    }

    @PostMapping
    String submit(@RequestParam(name = "title", required = false) String submittedTitle,
                  @RequestParam(name = "files", required = false) List<MultipartFile> files,
                  MultipartHttpServletRequest request,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response, RedirectAttributes redirect) throws IOException {
        response.addHeader("Vary", "HX-Request");
        String title = submittedTitle == null ? "" : submittedTitle;
        model.addAttribute("submissionTitle", title);

        if (title.isBlank()) {
            model.addAttribute("titleError", "タイトルを入力してください。");
        } else if (title.codePointCount(0, title.length()) > 60) {
            model.addAttribute("titleError", "タイトルは60文字以内で入力してください。");
        }

        long fileCount = request.getMultiFileMap().values().stream().mapToLong(List::size).sum();
        if (fileCount != 1 || files == null || files.size() != 1) {
            model.addAttribute("fileError", "UTF-8 のテキストファイルを1件選択してください。");
        } else {
            MultipartFile file = files.getFirst();
            var problem = UploadedTextFileValidator.validate(file);
            if (problem.isPresent()) {
                var validationProblem = problem.get();
                if (validationProblem.statusCode() == PAYLOAD_TOO_LARGE) {
                    response.setStatus(PAYLOAD_TOO_LARGE);
                    model.addAttribute("status", PAYLOAD_TOO_LARGE);
                    return "error";
                }
                model.addAttribute("fileError", validationProblem.message());
            }
        }

        boolean invalid = model.containsAttribute("titleError") || model.containsAttribute("fileError");
        if (invalid) {
            response.setStatus(UNPROCESSABLE_ENTITY);
            return htmx ? "examples/file-upload-input :: uploadForm" : "examples/file-upload-input";
        }

        String fileName = UploadedTextFileValidator.displayName(files.getFirst().getOriginalFilename());
        String message = "「" + title.trim() + "」として「" + fileName + "」を確認しました。"
                + "ファイルは保存せず、要求終了後に破棄します。";
        if (!htmx) {
            redirect.addFlashAttribute("uploadNotice", message);
            return "redirect:/examples/file-upload-input";
        }
        model.addAttribute("submissionTitle", "");
        model.addAttribute("uploadSuccess", message);
        return "examples/file-upload-input :: uploadForm";
    }
}
