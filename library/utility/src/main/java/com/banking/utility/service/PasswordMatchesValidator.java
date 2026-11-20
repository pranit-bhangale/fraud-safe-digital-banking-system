package com.banking.utility.service;

import com.banking.utility.annotation.PasswordMatches;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Validates that two properties of a bean hold the same value.
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private final Map<Class<?>, Accessors> cache = new ConcurrentHashMap<>();
    private String passwordProperty;
    private String confirmProperty;

    private static boolean matches(final String a, final String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static Function<Object, Object> accessor(final Class<?> type, final String name) {
        final String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (final String methodName : new String[]{name, "get" + capitalized}) {
            try {
                final Method method = type.getMethod(methodName);
                method.trySetAccessible();
                return target -> invoke(method, target);
            } catch (final NoSuchMethodException ignored) {

            }
        }

        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                final Field field = c.getDeclaredField(name);
                field.trySetAccessible();
                return target -> read(field, target);
            } catch (final NoSuchFieldException ignored) {

            }
        }

        throw new IllegalStateException("@PasswordMatches: property '" + name + "' not found on " + type.getName());
    }

    private static Object invoke(final Method method, final Object target) {
        try {
            return method.invoke(target);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read property via " + method.getName(), e);
        }
    }

    private static Object read(final Field field, final Object target) {
        try {
            return field.get(target);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException("Cannot read field " + field.getName(), e);
        }
    }

    @Override
    public void initialize(final PasswordMatches annotation) {
        this.passwordProperty = annotation.password();
        this.confirmProperty = annotation.confirmPassword();
    }

    @Override
    public boolean isValid(final Object bean, final ConstraintValidatorContext context) {
        if (bean == null) {
            return true;
        }
        final Accessors accessors = this.cache.computeIfAbsent(bean.getClass(), this::resolve);
        final Object password = accessors.password().apply(bean);
        final Object confirm = accessors.confirm().apply(bean);

        // Both empty: @NotBlank on the fields reports it, avoid a duplicate message
        if (password == null && confirm == null) {
            return true;
        }
        if (password != null && confirm != null && matches(password.toString(), confirm.toString())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(this.confirmProperty)
                .addConstraintViolation();
        return false;
    }

    private Accessors resolve(final Class<?> type) {
        return new Accessors(accessor(type, this.passwordProperty), accessor(type, this.confirmProperty));
    }

    private record Accessors(Function<Object, Object> password, Function<Object, Object> confirm) {
    }
}