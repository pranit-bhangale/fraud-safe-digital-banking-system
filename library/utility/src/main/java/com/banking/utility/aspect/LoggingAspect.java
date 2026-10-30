package com.banking.utility.aspect;

import com.banking.utility.annotation.LogExecution;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/**
 * Aspect that logs the execution time of methods annotated with {@link LogExecution}.
 */
@Aspect
public class LoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@annotation(com.banking.utility.annotation.LogExecution)")
    public Object logMethodExecution(final ProceedingJoinPoint joinPoint) throws Throwable {
        final String methodName = joinPoint.getSignature().toShortString();
        final Instant startTime = Instant.now();
        LOGGER.info("Initiating execution of method: {} | Start Time: {}", methodName, startTime);
        try {
            final Object result = joinPoint.proceed();
            final Instant finishTime = Instant.now();
            final Duration totalTime = Duration.between(startTime, finishTime);
            LOGGER.info("Finished execution of method: {} | Start Time: {} | Finish Time: {} | Total Execution Time: {} ms",
                    methodName, startTime, finishTime, totalTime.toMillis());
            return result;
        } catch (final Throwable exception) {
            final Instant finishTime = Instant.now();
            final Duration totalTime = Duration.between(startTime, finishTime);
            LOGGER.error("Terminating execution of method: {} due to error | Start Time: {} | Finish Time: {} | Total Execution Time: {} ms",
                    methodName, startTime, finishTime, totalTime.toMillis(), exception);
            throw exception;
        }
    }
}
