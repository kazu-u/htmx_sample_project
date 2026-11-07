package com.example.htmx.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/** セッション上限を予期しない内部エラーではなく、一時的な利用不可として案内する。 */
@ControllerAdvice(annotations = Controller.class)
class SessionCapacityExceededAdvice {
    @ExceptionHandler(SessionCapacityExceededException.class)
    ModelAndView handleSessionCapacityExceeded() {
        var view = new ModelAndView("error");
        view.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
        view.addObject("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        return view;
    }
}
