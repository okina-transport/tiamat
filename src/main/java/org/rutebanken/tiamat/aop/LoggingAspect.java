package org.rutebanken.tiamat.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Value("${logging.method-execution-duration.threshold:5000}")
    private long threshold;

    @Pointcut("execution(* org.rutebanken.tiamat.repository..*(..)))")
    public void repositoryMethods() {}

    @Pointcut("execution(* org.rutebanken.tiamat.service..*(..)))")
    public void serviceMethods() {}

    @Pointcut("repositoryMethods() || serviceMethods()")
    public void targetMethods() {}

    @Around("targetMethods()")
    public Object profileAllMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();

        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();

        final StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        Object result = proceedingJoinPoint.proceed();
        stopWatch.stop();

        if (stopWatch.getTotalTimeMillis() > threshold) {
            logger.info("Execution time of {}.{} : {} ms", className, methodName, stopWatch.getTotalTimeMillis());
        }

        return result;
    }
}
