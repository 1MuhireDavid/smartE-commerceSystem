package org.ecommerce.api.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * AOP aspect that centrally logs every exception thrown within the API layer
 * ({@code org.example.api}) before it propagates to the caller.
 *
 * <h2>Why AOP for exception logging?</h2>
 * <p>Without AOP, catching-and-rethrowing in every method pollutes business
 * logic with infrastructure concerns. This aspect adds structured exception
 * logging as a pure cross-cutting concern: no service or controller class
 * needs to change.
 *
 * <h2>Advice type: @AfterThrowing</h2>
 * <p>{@code @AfterThrowing} fires only when the joined method exits by throwing
 * an exception — not on normal return. The {@code throwing} attribute binds the
 * thrown object to the {@code ex} parameter so the advice can inspect it.
 * The exception <strong>always propagates</strong> after this advice; it cannot
 * be swallowed here (use {@code @Around} for that).
 *
 * <h2>Two log levels</h2>
 * <ul>
 *   <li><strong>WARN</strong> — for expected domain errors ({@link ResponseStatusException}
 *       with 4xx codes). These are user/client errors, not bugs.</li>
 *   <li><strong>ERROR</strong> — for all other throwables (unexpected bugs,
 *       infrastructure failures). Includes the full stack trace.</li>
 * </ul>
 *
 * <h2>Pointcut scope</h2>
 * <p>Targets every public method in {@code org.example.api} and its sub-packages.
 * This covers services, controllers, and GraphQL resolvers — any layer that can
 * throw an exception worth recording.
 */
@Aspect
@Component
public class ExceptionLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ExceptionLoggingAspect.class);

    /**
     * Matches every public method anywhere in the API layer.
     * Wider than the service-only pointcuts so that controller-level errors
     * (e.g., missing path variables, type mismatches) are also captured.
     */
    @Pointcut("execution(public * org.example.api..*(..))")
    public void apiLayerPublicMethods() {}

    /**
     * Logs every {@link Throwable} thrown by an API-layer method.
     *
     * <p>The {@code throwing = "ex"} attribute tells Spring AOP to bind the
     * thrown exception to the {@code ex} parameter. The type declared here
     * ({@link Throwable}) acts as a filter — only exceptions assignable to
     * {@code Throwable} will trigger this advice (which is everything).
     *
     * <p><strong>Important:</strong> this advice does <em>not</em> catch or
     * wrap the exception. It merely observes it. The exception continues
     * propagating to the next handler (e.g., {@code GlobalExceptionHandler}).
     *
     * @param joinPoint metadata about the method that threw
     * @param ex        the thrown exception (bound via the {@code throwing} attribute)
     */
    @AfterThrowing(pointcut = "apiLayerPublicMethods()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        MethodSignature sig        = (MethodSignature) joinPoint.getSignature();
        String          className  = joinPoint.getTarget().getClass().getSimpleName();
        String          methodName = sig.getName();

        if (ex instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            // Expected client errors — WARN, no stack trace
            log.warn("✗ CLIENT ERROR in {}.{}() → {} {}",
                     className, methodName,
                     rse.getStatusCode().value(),
                     rse.getReason());
        } else {
            // Unexpected errors — ERROR with full stack trace
            log.error("✗ EXCEPTION in {}.{}() → {} : {}",
                      className, methodName,
                      ex.getClass().getSimpleName(),
                      ex.getMessage(),
                      ex);
        }
    }
}
