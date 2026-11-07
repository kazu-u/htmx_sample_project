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
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 一時的に受信したテキストを検証し、内容を保持せず結果だけ返す。 */
@Controller
@RequestMapping("/examples/file-upload")
public class FileUploadController {
    private static final int PAYLOAD_TOO_LARGE = 413;
    private static final int UNPROCESSABLE_ENTITY = 422;

    @GetMapping
    String index() {
        return "examples/file-upload";
    }

    @PostMapping
    String upload(@RequestParam(name = "files", required = false) List<MultipartFile> files,
                  MultipartHttpServletRequest request,
                  @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmx,
                  Model model, HttpServletResponse response, RedirectAttributes redirect) throws IOException {
        response.addHeader("Vary", "HX-Request");
        long fileCount = request.getMultiFileMap().values().stream().mapToLong(List::size).sum();
        if (fileCount != 1 || files == null || files.size() != 1) {
            return invalid("UTF-8 のテキストファイルを1件選択してください。", htmx, model, response);
        }

        MultipartFile file = files.getFirst();
        var problem = UploadedTextFileValidator.validate(file);
        if (problem.isPresent()) {
            var validationProblem = problem.get();
            if (validationProblem.statusCode() == PAYLOAD_TOO_LARGE) {
                return tooLarge(model, response);
            }
            return invalid(validationProblem.message(), htmx, model, response);
        }
        String fileName = UploadedTextFileValidator.displayName(file.getOriginalFilename());

        String message = "「" + fileName + "」（" + file.getSize()
                + " bytes）を検証しました。ファイルは保存せず、要求終了後に破棄します。";
        if (!htmx) {
            redirect.addFlashAttribute("uploadNotice", message);
            return "redirect:/examples/file-upload";
        }
        model.addAttribute("uploadName", fileName);
        model.addAttribute("uploadSize", file.getSize());
        return "examples/file-upload :: uploadResult";
    }

    private String invalid(String message, boolean htmx, Model model, HttpServletResponse response) {
        response.setStatus(UNPROCESSABLE_ENTITY);
        model.addAttribute("uploadError", message);
        return htmx ? "examples/file-upload :: uploadResult" : "examples/file-upload";
    }

    private String tooLarge(Model model, HttpServletResponse response) {
        response.setStatus(PAYLOAD_TOO_LARGE);
        model.addAttribute("status", PAYLOAD_TOO_LARGE);
        return "error";
    }

}
