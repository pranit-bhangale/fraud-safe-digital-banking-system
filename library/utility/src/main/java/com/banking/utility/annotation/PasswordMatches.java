package com.banking.utility.annotation;

import com.banking.utility.service.PasswordMatchesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class-level constraint: the {@link #password()} and {@link #confirmPassword()}
 * properties of the annotated type must be equal.
 */
@Documented
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface PasswordMatches {

    String message() default "Password and confirm password do not match";

    String password() default "password";

    String confirmPassword() default "confirmPassword";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}