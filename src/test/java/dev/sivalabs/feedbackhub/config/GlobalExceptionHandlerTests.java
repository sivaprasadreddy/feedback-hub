package dev.sivalabs.feedbackhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sivalabs.feedbackhub.shared.BadRequestException;
import dev.sivalabs.feedbackhub.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsMatchingHttpStatusForEachErrorView() {
        assertError(handler.handle(new BadRequestException("bad request")), HttpStatus.BAD_REQUEST, "error/400");
        assertError(handler.handle(new ResourceNotFoundException("not found")), HttpStatus.NOT_FOUND, "error/404");
        assertError(handler.handle(new AccessDeniedException("forbidden")), HttpStatus.FORBIDDEN, "error/403");
        assertError(
                handler.handle(new IllegalStateException("unexpected")), HttpStatus.INTERNAL_SERVER_ERROR, "error/500");
    }

    private void assertError(
            org.springframework.web.servlet.ModelAndView result, HttpStatus expectedStatus, String expectedView) {
        assertThat(result.getStatus()).isEqualTo(expectedStatus);
        assertThat(result.getViewName()).isEqualTo(expectedView);
    }
}
