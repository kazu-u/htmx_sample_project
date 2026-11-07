package com.example.htmx.examples.fileupload;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/** MVC が要求サイズ超過を検出した場合も、HTTP 413 と案内ページを返す。 */
@ControllerAdvice
class UploadLimitAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ModelAndView fileExceedsConfiguredLimit() {
        var view = new ModelAndView("error");
        view.setStatus(HttpStatusCode.valueOf(413));
        view.addObject("status", 413);
        return view;
    }
}
