package com.taskhub.taskservice.common;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void should_return404ProblemDetail_when_resourceNotFound() {
        ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException("Project not found: x"));

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("Project not found: x");
    }

    @Test
    void should_return400ProblemDetail_when_invalidReference() {
        ProblemDetail problem = handler.handleInvalidReference(new InvalidReferenceException("Owner not found: x"));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Owner not found: x");
    }

    @Test
    void should_return400ProblemDetailWithFieldErrors_when_validationFails() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "projectRequest");
        bindingResult.addError(new FieldError("projectRequest", "name", "must not be blank"));

        MethodParameter methodParameter =
                new MethodParameter(GlobalExceptionHandlerTests.class.getDeclaredMethod("dummy", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ProblemDetail problem = handler.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getProperties()).containsKey("errors");
    }

    @Test
    void should_return500ProblemDetailWithoutLeakingDetails_when_unhandledException() {
        ProblemDetail problem = handler.handleGeneric(new RuntimeException("boom, contains secret info"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).doesNotContain("boom", "secret");
    }

    @SuppressWarnings("unused")
    private void dummy(String name) {
    }
}
