package com.busbooking.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

/**
 * Turns every exception into an RFC 7807 JSON body with an extra {@code code} field, e.g.
 * <pre>{"status":409,"title":"Conflict","detail":"Seat 3A was just booked…","code":"SEAT_TAKEN"}</pre>
 * Validation errors also carry {@code errors: [{field, message}]} for showing next to form fields.
 * Unexpected errors are logged with their stack trace but answered with a generic message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex) {
        return problem(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .toList();
        ProblemDetail body = problem(ErrorCode.VALIDATION_FAILED, "Some fields are not valid");
        body.setProperty("errors", errors);
        return body;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleParamValidation(HandlerMethodValidationException ex) {
        return problem(ErrorCode.VALIDATION_FAILED, "Some request parameters are not valid");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ProblemDetail handleBadRequest(Exception ex) {
        String detail = ex instanceof HttpMessageNotReadableException
                ? "The request body is missing or is not valid JSON"
                : ex.getMessage();
        return problem(ErrorCode.BAD_REQUEST, detail);
    }

    /** A database constraint stopped the change, e.g. a unique value that already exists. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(ErrorCode.CONFLICT, "This change conflicts with existing data");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        return problem(ErrorCode.NOT_FOUND, "Not found");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(ErrorCode.INTERNAL_ERROR, "Something went wrong on our side. Please try again");
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        HttpStatus status = code.status();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty("code", code.name());
        return body;
    }
}
