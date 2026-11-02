/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.error;

import com.eshabakhov.surok.federal.curriculum.CurriculumAlreadyExistsException;
import com.eshabakhov.surok.federal.curriculum.CurriculumFailedCreateException;
import com.eshabakhov.surok.federal.curriculum.CurriculumNotFoundException;
import com.eshabakhov.surok.federal.curriculum.input.CurriculumInputException;
import com.eshabakhov.surok.federal.curriculum.requirement.RequirementAlreadyExistsException;
import com.eshabakhov.surok.federal.curriculum.requirement.RequirementNotFoundException;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RequirementInputException;
import com.eshabakhov.surok.federal.subject.SubjectAlreadyExistsException;
import com.eshabakhov.surok.federal.subject.SubjectFailedCreateException;
import com.eshabakhov.surok.federal.subject.SubjectNotFoundException;
import com.eshabakhov.surok.federal.subject.SubjectReadOnlyException;
import com.eshabakhov.surok.federal.subject.input.SubjectInputException;
import com.eshabakhov.surok.school.SchoolFailedCreateException;
import com.eshabakhov.surok.school.SchoolNotFoundException;
import com.eshabakhov.surok.school.building.BuildingAlreadyExistsException;
import com.eshabakhov.surok.school.building.BuildingNotFoundException;
import com.eshabakhov.surok.school.building.cabinet.CabinetAlreadyExistsException;
import com.eshabakhov.surok.school.building.cabinet.CabinetFailedCreateException;
import com.eshabakhov.surok.school.building.cabinet.CabinetNotFoundException;
import com.eshabakhov.surok.school.building.cabinet.input.CabinetInputException;
import com.eshabakhov.surok.school.building.input.BuildingInputException;
import com.eshabakhov.surok.school.input.SchoolInputException;
import com.eshabakhov.surok.school.schedule.ScheduleAlreadyExistsException;
import com.eshabakhov.surok.school.schedule.ScheduleFailedCreateException;
import com.eshabakhov.surok.school.schedule.ScheduleNotFoundException;
import com.eshabakhov.surok.school.schedule.input.ScheduleInputException;
import com.eshabakhov.surok.school.schedule.load.LoadNotFoundException;
import com.eshabakhov.surok.school.schedule.load.autofill.LoadDraftNotFoundException;
import com.eshabakhov.surok.school.schedule.load.autofill.LoadInputException;
import com.eshabakhov.surok.school.schoolclass.SchoolClassAlreadyExistsException;
import com.eshabakhov.surok.school.schoolclass.SchoolClassFailedCreateException;
import com.eshabakhov.surok.school.schoolclass.SchoolClassNotFoundException;
import com.eshabakhov.surok.school.schoolclass.input.SchoolClassInputException;
import com.eshabakhov.surok.school.teacher.TeacherAlreadyExistsException;
import com.eshabakhov.surok.school.teacher.TeacherFailedCreateException;
import com.eshabakhov.surok.school.teacher.TeacherNotFoundException;
import com.eshabakhov.surok.school.teacher.input.TeacherInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler for REST controllers.
 *
 * <p>This class provides centralized exception handling for all
 * REST controllers in the application, including a last-resort resolver after
 * the MVC resolvers have checked specific exceptions and their causes.</p>
 *
 * @since 0.0.1
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings(
    "PMD.ProhibitPublicStaticMethods"
)
public final class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler
    implements HandlerExceptionResolver {

    private RestResponseEntityExceptionHandler() { }

    /**
     * Handle a missing request header.
     *
     * @param request Incoming request
     * @param exception Missing header exception
     * @return Bad request response
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public static ResponseEntity<String> handleMissingRequestHeaderException(
        final HttpServletRequest request, final MissingRequestHeaderException exception
    ) {
        return new ErHttp(request, HttpStatus.BAD_REQUEST, exception.getMessage()).response();
    }

    /**
     * Handle an invalid client request.
     *
     * @param request Incoming request
     * @param exception Client request exception
     * @return Bad request response
     */
    @ExceptionHandler(
        {
            SchoolInputException.class,
            BuildingInputException.class,
            CabinetInputException.class,
            ScheduleInputException.class,
            SchoolClassInputException.class,
            SubjectInputException.class,
            TeacherInputException.class,
            CurriculumInputException.class,
            LoadInputException.class,
            RequirementInputException.class
        }
    )
    public static ResponseEntity<String> handleClientException(
        final HttpServletRequest request, final Exception exception
    ) {
        return new ErHttp(request, HttpStatus.BAD_REQUEST, exception.getMessage()).response();
    }

    /**
     * Reject an unauthorized mutation of a shared subject.
     *
     * @param request Incoming request
     * @param exception Rejected mutation
     * @return Forbidden response
     */
    @ExceptionHandler(SubjectReadOnlyException.class)
    public static ResponseEntity<String> handleReadOnlySubject(
        final HttpServletRequest request, final SubjectReadOnlyException exception
    ) {
        return new ErHttp(request, HttpStatus.FORBIDDEN, exception.getMessage()).response();
    }

    /**
     * Handle a missing resource.
     *
     * @param request Incoming request
     * @return Not-found response
     */
    @ExceptionHandler(
        {
            CurriculumNotFoundException.class,
            RequirementNotFoundException.class,
            BuildingNotFoundException.class,
            CabinetNotFoundException.class,
            SchoolNotFoundException.class,
            SchoolClassNotFoundException.class,
            LoadNotFoundException.class,
            LoadDraftNotFoundException.class,
            ScheduleNotFoundException.class,
            SubjectNotFoundException.class,
            TeacherNotFoundException.class
        }
    )
    public static ResponseEntity<String> handleNotFoundException(final HttpServletRequest request) {
        return new ErHttp(request, HttpStatus.NOT_FOUND).response();
    }

    /**
     * Handle an invalid method argument type.
     *
     * @param request Incoming request
     * @return Not acceptable response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public static ResponseEntity<String> handleMethodArgumentTypeMismatchException(
        final HttpServletRequest request
    ) {
        return new ErHttp(request, HttpStatus.NOT_ACCEPTABLE).response();
    }

    /**
     * Handle a resource conflict.
     *
     * @param request Incoming request
     * @param exception Conflict exception
     * @return Conflict response
     */
    @ExceptionHandler(
        {
            CurriculumAlreadyExistsException.class,
            RequirementAlreadyExistsException.class,
            BuildingAlreadyExistsException.class,
            CabinetAlreadyExistsException.class,
            ScheduleAlreadyExistsException.class,
            SchoolClassAlreadyExistsException.class,
            SubjectAlreadyExistsException.class,
            TeacherAlreadyExistsException.class
        }
    )
    public static ResponseEntity<String> handleConflictException(
        final HttpServletRequest request, final Exception exception
    ) {
        return new ErHttp(request, HttpStatus.CONFLICT, exception.getMessage()).response();
    }

    /**
     * Handle an internal server error.
     *
     * @param request Incoming request
     * @return Internal server error response
     */
    @ExceptionHandler(
        {
            CurriculumFailedCreateException.class,
            CabinetFailedCreateException.class,
            SchoolFailedCreateException.class,
            SchoolClassFailedCreateException.class,
            ScheduleFailedCreateException.class,
            SubjectFailedCreateException.class,
            TeacherFailedCreateException.class
        }
    )
    public static ResponseEntity<String> handleServerException(final HttpServletRequest request) {
        return new ErHttp(request, HttpStatus.INTERNAL_SERVER_ERROR).response();
    }

    /**
     * Handle an authorization failure raised by a controller.
     *
     * @param request Incoming request
     * @return Forbidden response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public static ResponseEntity<String> handleAccessDeniedException(
        final HttpServletRequest request
    ) {
        return new ErHttp(request, HttpStatus.FORBIDDEN).response();
    }

    /**
     * Handle an authentication failure raised by a controller.
     *
     * @param request Incoming request
     * @return Authentication challenge
     */
    @ExceptionHandler(AuthenticationException.class)
    public static ResponseEntity<String> handleAuthenticationException(
        final HttpServletRequest request
    ) {
        final ResponseEntity<String> error = new ErHttp(
            request, HttpStatus.UNAUTHORIZED
        ).response();
        return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
            .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Surok\"").body(error.getBody());
    }

    @Override
    public ModelAndView resolveException(
        final HttpServletRequest request, final HttpServletResponse response,
        final Object handler, final Exception exception
    ) {
        this.logger.error("Request processing failed", exception);
        if (!response.isCommitted()) {
            final ResponseEntity<String> error = new ErHttp(
                request, HttpStatus.INTERNAL_SERVER_ERROR
            ).response();
            response.setStatus(error.getStatusCode().value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            error.getHeaders().forEach(
                (name, values) -> values.forEach(value -> response.addHeader(name, value))
            );
            if (error.getBody() != null) {
                try {
                    response.getWriter().write(error.getBody());
                } catch (final IOException failure) {
                    throw new UncheckedIOException("Failed to write the error response", failure);
                }
            }
        }
        return new ModelAndView();
    }

    @Override
    public ResponseEntity<Object> createResponseEntity(
        final Object body, final HttpHeaders headers, final HttpStatusCode status,
        final WebRequest request
    ) {
        final ResponseEntity<String> error = new ErHttp(
            ((ServletWebRequest) request).getRequest(), HttpStatus.valueOf(status.value())
        ).response();
        return ResponseEntity.status(status).headers(headers).headers(error.getHeaders())
            .body(error.getBody());
    }
}
