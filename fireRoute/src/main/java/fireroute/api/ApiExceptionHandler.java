package fireroute.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());

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