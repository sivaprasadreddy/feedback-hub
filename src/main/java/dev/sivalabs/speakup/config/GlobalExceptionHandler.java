package dev.sivalabs.speakup.config;

import dev.sivalabs.speakup.shared.BadRequestException;
import dev.sivalabs.speakup.shared.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    ModelAndView handle(BadRequestException e) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("message", e.getMessage());
        modelAndView.addObject("exception", e);
        modelAndView.setStatus(HttpStatus.BAD_REQUEST);
        modelAndView.setViewName("error/400");
        return modelAndView;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ModelAndView handle(ResourceNotFoundException e) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("message", e.getMessage());
        modelAndView.addObject("exception", e);
        modelAndView.setViewName("error/404");
        return modelAndView;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ModelAndView handle(AccessDeniedException e) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("message", e.getMessage());
        modelAndView.addObject("exception", e);
        modelAndView.setViewName("error/403");
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    ModelAndView handle(Exception e) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("message", e.getMessage());
        modelAndView.addObject("exception", e);
        modelAndView.setViewName("error/500");
        return modelAndView;
    }
}
