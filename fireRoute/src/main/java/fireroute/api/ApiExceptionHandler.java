package fireroute.api;

import fireroute.application.ShelterNotFoundException;
import fireroute.application.LocationOutsideCoverageException;
import fireroute.application.JunctionNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.beans.TypeMismatchException;
import jakarta.validation.ConstraintViolationException;

import java.util.LinkedHashMap;
import java.util.Map;

/*  Exception handler for API-level exceptions
Representation for an RFC 9457 problem detail. Includes spec-defined properties, 
and a properties map for additional, non-standard properties*/


/*
 * Extending ResponseEntityExceptionHandler is what keeps the catch-all below
 * honest. Spring answers an unknown path with 404 and a wrong verb with 405 by
 * raising its own exceptions, and a bare @ExceptionHandler(Exception.class)
 * intercepts every one of them and rebrands it a 500 — so a missing endpoint
 * reports itself as a broken server. The base class supplies the correct
 * handler for each, and Spring prefers the most specific match, so the
 * catch-all only ever sees what nothing else claimed.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = validationProblem(errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            errors.putIfAbsent(field, violation.getMessage());
        });
        return validationProblem(errors);
    }

    private ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setDetail("One or more request fields are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problem = badRequest("Missing required parameter: " + ex.getParameterName());
        problem.setProperty("parameter", ex.getParameterName());
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problem = badRequest("Parameter has an invalid value");
        if (ex.getPropertyName() != null) problem.setProperty("parameter", ex.getPropertyName());
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return badRequest(ex.getMessage());
    }

    private ProblemDetail badRequest(String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(detail);
        return problem;
    }

    @ExceptionHandler(JunctionNotFoundException.class)
    public ProblemDetail handleJunctionNotFound(JunctionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Junction Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(ShelterNotFoundException.class)
    public ProblemDetail handleShelterNotFound(ShelterNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Shelter Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(LocationOutsideCoverageException.class)
    public ProblemDetail handleLocationOutsideCoverage(LocationOutsideCoverageException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Location Outside Coverage");
        problem.setDetail(ex.getMessage());
        problem.setProperty("distanceMeters", Math.round(ex.getDistanceMeters()));
        problem.setProperty("maximumMeters", ex.getMaximumMeters());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.INTERNAL_SERVER_ERROR
                );

        problem.setTitle("Internal Server Error");
        problem.setDetail(
                "An unexpected error occurred"
        );

        // The client gets nothing but the status; the details belong in the log,
        // where they are useful, rather than in the response, where a stack trace
        // would hand a stranger the internal structure. Without this line the
        // cause of every 500 disappears without trace.
        log.error("Unhandled exception while serving a request", ex);

        return problem;
    }
}
