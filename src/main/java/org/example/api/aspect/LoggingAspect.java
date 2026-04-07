package org.example.api.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that logs method entry and exit for every public method in the
 * service implementation layer ({@code org.example.api.service.impl}).
 *
 * <h2>Why AOP for logging?</h2>
 * <p>Without AOP, every service method would need identical try/finally blocks
 * to log its entry and exit — duplicating the same boilerplate across dozens of
 * methods. AOP extracts that concern into this single class. Adding or removing
 * service-layer logging requires no changes to any service class.
 *
 * <h2>Advice types used</h2>
 * <ul>
 *   <li>{@link Before} — fires <em>before</em> the target method executes.
 *       Logs the class name, method name, and argument count so we know what
 *       was called and with how many inputs.</li>
 *   <li>{@link After} — fires <em>after</em> the target method returns
 *       <strong>or</strong> throws an exception (equivalent to a finally block).
 *       Guarantees a paired exit log even when the method fails.</li>
 * </ul>
 *
 * <h2>Pointcut design</h2>
 * <p>The pointcut targets {@code execution(public * org.example.api.service.impl.*.*(..))},
 * which matches every public method in every class under the {@code service.impl}
 * package. Restricting to {@code impl} (not the interfaces) avoids double-firing
 * on interface proxy calls.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Matches every public method in every service implementation class.
     * Declared as a named pointcut so it can be referenced by both advice methods
     * without duplicating the expression string.
     */
    @Pointcut("execution(public * org.example.api.service.impl.*.*(..))")
    public void serviceImplMethods() {}

    /**
     * Logs method entry before execution begins.
     *
     * <p>Uses {@link JoinPoint} to inspect the target class and method signature
     * at runtime without hard-coding any class names.
     *
     * @param joinPoint metadata about the intercepted method call
     */
    @Before("serviceImplMethods()")
    public void logMethodEntry(JoinPoint joinPoint) {
        MethodSignature sig        = (MethodSignature) joinPoint.getSignature();
        String          className  = joinPoint.getTarget().getClass().getSimpleName();
        String          methodName = sig.getName();
        int             argCount   = joinPoint.getArgs().length;

        log.debug("→ ENTER  {}.{}()  [args: {}]", className, methodName, argCount);
    }

    /**
     * Logs method exit after execution completes (or throws).
     *
     * <p>{@code @After} always fires — it is the AOP equivalent of a
     * {@code finally} block. For execution-time measurement see
     * {@link PerformanceMonitoringAspect}.
     *
     * @param joinPoint metadata about the intercepted method call
     */
    @After("serviceImplMethods()")
    public void logMethodExit(JoinPoint joinPoint) {
        MethodSignature sig        = (MethodSignature) joinPoint.getSignature();
        String          className  = joinPoint.getTarget().getClass().getSimpleName();
        String          methodName = sig.getName();

        log.debug("← EXIT   {}.{}()", className, methodName);
    }
}
